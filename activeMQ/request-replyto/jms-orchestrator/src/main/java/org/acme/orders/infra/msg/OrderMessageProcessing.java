package org.acme.orders.infra.msg;


import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.acme.orders.domain.Order;
import org.acme.orders.domain.OrderService;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
/**
 * OrderMessageProcessing is a producer to the orders queue and consumer on replyTo queue
 */
@ApplicationScoped
public class OrderMessageProcessing implements Runnable, MessageListener, ExceptionListener {
    Logger logger = Logger.getLogger(OrderMessageProcessing.class.getName());

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

    /*
     * One connection to the JMS provider, one session to send message and another one 
     * to asynchronously receive the reply.
     */
    private synchronized void connect() throws JMSException {
        if (! isConnected()) {
            connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
            connection = connectionFactory.createConnection(user, password);
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
            initProducer();
            initConsumer();
            connection.start();
            logger.info("Connect to broker succeed");
        } 
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
        closeUtil(connection);
        consumer = null;
        producerSession = null;
        consumerSession = null;
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
            connect();
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
            && consumer != null 
            && producerSession != null 
            && producer != null
            && consumerSession != null;
    }

    public void sendMessage(Order order)  {
       
        try {
            if (! isConnected()) {
                connect();
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
        catch (JsonProcessingException | JMSException e) {
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
                connect();
            } catch (JMSException e) {  
                logger.info("Reconnect to broker fails");
                disconnect();
                reconnect(delay + 5);
            }
        } , delay, TimeUnit.SECONDS);
        
    }
}
