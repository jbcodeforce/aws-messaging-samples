package org.acme.orders.infra.msg;


import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.acme.orders.domain.Order;
import org.acme.orders.domain.OrderService;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.transport.TransportListener;
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
 * OrderMessageProcessing is a producer to the orders queue and consumer on replyTo queue
 */
@ApplicationScoped
public class OrderMessageProcessor implements Runnable, MessageListener, ExceptionListener, TransportListener {
    Logger logger = Logger.getLogger(OrderMessageProcessor.class.getName());

    @Inject
    @ConfigProperty(name="main.queue.name")
    public String mainQueueName;

    @Inject
    @ConfigProperty(name="replyTo.queue.name")
    public String replyToQueueName;

    @Inject
    @ConfigProperty(name="reconnect.delay.ins")
    public int reconnectDelay;

    @Inject
    @ConfigProperty(name="quarkus.artemis.url")
    public String connectionURLs;
    
    @Inject
    @ConfigProperty(name="quarkus.artemis.username")
    private String user;

    @Inject
    @ConfigProperty(name="quarkus.artemis.password")
    private String password;


    @Inject
    private OrderService service;

    private static ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService simulatorScheduler, reconnectScheduler = null;
    private ConnectionFactory connectionFactory;
    private Connection connection = null;
    private  MessageProducer producer;
    private MessageConsumer consumer;
    private Queue mainQueue;
    private Queue replyToQueue; 
    private MessageConsumer messageConsumer;
    private Session producerSession;
    private Session consumerSession;
    private boolean isLeveragingFailoverProtocol = false;
    /*
     * One connection to the JMS provider, one session to send message and another one 
     * to asynchronously receive the reply.
     */
    private synchronized void restablishConnection() throws JMSException {
        if (connection == null) {
            connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
            connection = connectionFactory.createConnection(user, password);
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
            ((ActiveMQConnection) connection).addTransportListener(this);
        } 
        if (producer == null || producerSession == null)
            initProducer();
        if (consumer == null || consumerSession == null) 
            initConsumer();
        connection.start();
        logger.info("Connect to broker succeed");
    }

    private void initProducer() throws JMSException{
        producerSession = connection.createSession();
        mainQueue = producerSession.createQueue(mainQueueName);
        producer = producerSession.createProducer(mainQueue);
        producer.setTimeToLive(60000); // one minute
    }

    private void initConsumer() throws JMSException {
        consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
        replyToQueue = consumerSession.createQueue(replyToQueueName);
        messageConsumer = consumerSession.createConsumer(replyToQueue);
        messageConsumer.setMessageListener(this);
    }

    private synchronized void disconnect() {
        closeUtil(consumer);
        closeUtil(consumerSession);
        closeUtil(producerSession);
        closeUtil(producer);
        closeUtil(connection);
        consumer = null;
        producerSession = null;
        consumerSession = null;
        producer = null;
        connection = null;
    }

    private void closeUtil(AutoCloseable ac) {
        try {
            if (ac != null) ac.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Use following code to start automatically when app starts 
     */
    void onStart(@Observes StartupEvent ev) {
        try {
            restablishConnection();
            if (connectionURLs.contains("failover")){
                isLeveragingFailoverProtocol=true;
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    void onStop(@Observes ShutdownEvent ev) {
        if (simulatorScheduler != null) 
            simulatorScheduler.shutdownNow();
        disconnect();
    }
    

    public void startSimulation(long delay) {
        if (simulatorScheduler == null) 
            simulatorScheduler = Executors.newSingleThreadScheduledExecutor();
        simulatorScheduler.scheduleWithFixedDelay(this, 0L, delay, TimeUnit.SECONDS);
    }


    /**
     * Generate an order each time it is called, this is for continuously simulate
     * order comming. It is for simulation purpose only, and controlled by SimulControl.
     */
    @Override
    public void run() {
        Order o = Order.buildOrder();
            sendMessage(o);

    }

    public boolean isConnected() {
        return connection != null 
            //&& consumer != null 
            && producerSession != null 
            && consumerSession != null;
    }

    public void sendMessage(Order order)  {
       
        try {
            if (! isLeveragingFailoverProtocol &&  ! isConnected()) {
                restablishConnection();
            }
            OrderMessage oe = OrderMessage.fromOrder(order);
            String orderJson= mapper.writeValueAsString(oe);
            TextMessage msg =  producerSession.createTextMessage(orderJson);
            msg.setJMSCorrelationID(UUID.randomUUID().toString().substring(0,8));
            producer.send( msg);
            logger.info("Send message to participant: " + orderJson);
               
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        } 
    }

    @Override
    public void onMessage(Message msg) {
       TextMessage rawMsg = (TextMessage) msg;
       OrderMessage om;
        try {
            om = mapper.readValue(rawMsg.getText(),OrderMessage.class);
            logger.info("Received message: " + om.toString());
            Order o = OrderMessage.toOrder(om);
            service.processParticipantResponse(o);
            msg.acknowledge();
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        catch (JMSException e) {
            disconnect();
            reconnect(reconnectDelay);
        }
    }
    

    @Override
    public void onException(JMSException e) {
        logger.error("JMS Exception occured: " + e.getMessage());
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

    @Override
    public void onCommand(Object arg0) {
        // not sure what to do.. order a pizza

        logger.debug("Unimplemented method 'onCommand'");
    }

    @Override
    public void onException(IOException arg0) {
        logger.info("Exception from transport protocol");
        disconnect();
        reconnect(reconnectDelay);
    }

    @Override
    public void transportInterupted() {
        logger.info("Transport interrupted ... it should recover...");
    }

    @Override
    public void transportResumed() {
        logger.info("Transport resumed ... we were right to wait...");
    }
}
