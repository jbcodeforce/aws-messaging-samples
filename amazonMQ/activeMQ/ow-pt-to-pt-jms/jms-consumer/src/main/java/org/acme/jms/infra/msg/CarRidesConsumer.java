package org.acme.jms.infra.msg;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.acme.jms.model.CarRide;
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


/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class CarRidesConsumer implements MessageListener, ExceptionListener{
    Logger logger = Logger.getLogger(CarRidesConsumer.class.getName());
    
    @Inject
    @ConfigProperty(name="queue.name")
    public String inQueueName;
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
    private MessageConsumer messageConsumer;
    private Session consumerSession;
    private Destination inQueue;
    private JMSContext jmsContext = null;
    private static ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService reconnectScheduler = null;
    private volatile String lastCarRide;

    public String getLastCarRide() {
        return lastCarRide;
    }

    void onStart(@Observes StartupEvent ev) {
        try {
            restablishConnection();
        } catch (JMSException e) {
          e.printStackTrace();
          reconnect(reconnectDelay);
        }
        logger.info("JMS Consumer Started");
    }

    void onStop(@Observes ShutdownEvent ev) {
        disconnect();
    }

    private synchronized void restablishConnection() throws javax.jms.JMSException {
        if (connection == null) {
            displayParameters();
            connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
            connection = connectionFactory.createConnection(user, password);
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
        }
        if (messageConsumer == null || consumerSession == null) {
            jmsContext = connectionFactory.createContext();
            consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
            //ActiveMQJMSConstants.INDIVIDUAL_ACKNOWLEDGE
            inQueue = consumerSession.createQueue(inQueueName);
            messageConsumer = consumerSession.createConsumer(inQueue);
            messageConsumer.setMessageListener(this);
        }
            

        connection.start();
        logger.info("Connect to broker succeed");
    }

    @Override
    public void onException(JMSException e) {
        logger.info("Exception from transport protocol");
        disconnect();
        reconnect(reconnectDelay);
    }

    @Override
    public void onMessage(Message msg) {
        if (msg == null) {
            return;
        }

       TextMessage rawMsg = (TextMessage) msg;
       CarRide om;
        try {
            om = mapper.readValue(rawMsg.getText(),CarRide.class);
            // do something with the car ride
            lastCarRide = rawMsg.getText();
            logger.info("Received message: " + lastCarRide);
            msg.acknowledge();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch ( JMSException e) {
            e.printStackTrace();
        }
    }


    private synchronized void disconnect() {
        closeUtil(consumerSession);
        closeUtil(connection);
        connection = null;
    }

    private void closeUtil(AutoCloseable ac) {
        try {
            if (ac != null) ac.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void displayParameters() {

        logger.info("##########  Connection parameters #######");
        logger.info("Hostname URL: " + connectionURLs);
        logger.info("Queue: " +  inQueueName);
        logger.info("App User: " +  user);
        logger.debug("App Password: " + password);
      }

    

 
}
