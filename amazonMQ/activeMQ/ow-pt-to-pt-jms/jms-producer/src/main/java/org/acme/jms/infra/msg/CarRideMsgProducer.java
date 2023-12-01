package org.acme.jms.infra.msg;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.acme.jms.model.CarRide;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;


/**
 * A bean JMS queue.
 */
@ApplicationScoped
public class CarRideMsgProducer implements Runnable, ExceptionListener {
    Logger logger = Logger.getLogger(CarRideMsgProducer.class.getName());
    
   
    @Inject
    @ConfigProperty(name="queue.name")
    public String outQueueName;
    @Inject
    @ConfigProperty(name="reconnect.delay.ins")
    public int reconnectDelay;
    @Inject
    @ConfigProperty(name="activemq.url")
    public String connectionURLs;
    @Inject
    @ConfigProperty(name="activemq.username")
    private String user;
    @Inject
    @ConfigProperty(name="activemq.password")
    private String password;
    
    @Inject
    @ConfigProperty(name = "app.name", defaultValue = "TestApp")
    public String appName;


    private ConnectionFactory connectionFactory;
    private Connection connection = null;
    private MessageProducer producer = null;
    private Session producerSession;
    private Queue outQueue;
    private JMSContext jmsContext = null;

    private final ScheduledExecutorService simulatorScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService reconnectScheduler = null;
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Listen to application started event to establich connection to MQ broker
     * @param ev
     */
    void onStart(@Observes StartupEvent ev) {
        try {
            restablishConnection();
        } catch (JMSException e) {
          e.printStackTrace();
          reconnect(reconnectDelay);
        }
        logger.info("JMS Producer Started");
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (simulatorScheduler != null) 
            simulatorScheduler.shutdownNow();
        if (reconnectScheduler != null)
            reconnectScheduler.shutdownNow();
        disconnect();
    }

    private synchronized void disconnect() {
        closeUtil(producerSession);
        closeUtil(connection);
        producerSession = null;
        producer = null;
        connection = null;
    }

    private synchronized void restablishConnection() throws JMSException {
        if (connection == null) {
            displayParameters();
            connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
            connection = connectionFactory.createConnection(user, password);
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
        } 
        if (producer == null || producerSession == null) {
            producerSession = connection.createSession();
            outQueue = producerSession.createQueue(outQueueName);
            producer = producerSession.createProducer(outQueue);
            producer.setTimeToLive(60000); // one minute
        }
        connection.start();
        logger.info("Connect to broker succeed");
    }

    public boolean isConnected() {
        return connection != null 
            && producerSession != null;
    }

    public void start(long delay) {
        simulatorScheduler.scheduleWithFixedDelay(this, 0L, delay, TimeUnit.SECONDS);
    }

    public void stop() {
        simulatorScheduler.shutdown();
    }

    
   
    @Override
    public void run() {
            CarRide cr = CarRide.createRandomQRide();
            sendCarRideEvent(cr);
    }

    public void sendNmessages(int totalMessageToSend) throws InterruptedException {
        logger.info("Sending " + totalMessageToSend + " messages");
        
        for (int i = 0; i < totalMessageToSend; i++) {
                run();
                Thread.sleep(500);
        }
    }

    public void sendCarRideEvent(CarRide cr) {
        try {
            String rideJson= mapper.writeValueAsString(cr);
            TextMessage msg =  producerSession.createTextMessage(rideJson);
            msg.setJMSMessageID(UUID.randomUUID().toString());
            producer.send(msg);
            logger.info("Sent: " + rideJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onException(JMSException arg0) {
        logger.error("JMS Exception occured: " + arg0.getMessage());
        disconnect();
        reconnect(reconnectDelay);
    }

    private void reconnect(int delay) {
        if (reconnectScheduler == null) 
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        reconnectScheduler.schedule( () -> {
            try {
                restablishConnection();
            } catch (JMSException e) {  
                logger.info("Reconnect to broker fails, retrying in " + delay + " s.");
                disconnect();
                reconnect(delay + 5);
            }
        } , delay, TimeUnit.SECONDS);
        
    }

    private void closeUtil(AutoCloseable ac) {
        try {
            if (ac != null) ac.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   

  private void displayParameters() {

    logger.info("##########  Connection parameters #######");
    logger.info("Hostname URL: " + connectionURLs);
    logger.info("Queue: " +  outQueueName);
    logger.info("App User: " +  user);
    logger.debug("App Password: " + password);
  }


}
