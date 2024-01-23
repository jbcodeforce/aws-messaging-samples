package org.acme.participant.infra.msg;

import java.io.IOException;
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
 * A OrderMessage Consumer to participate to the order process asynchrnously
 */
@ApplicationScoped
public class OrderMessageParticipantProcessor implements MessageListener, ExceptionListener, TransportListener {
    Logger logger = Logger.getLogger(OrderMessageParticipantProcessor.class.getName());
    
    @Inject
    @ConfigProperty(name="in.queue.name")
    public String inQueueName;
    @Inject
    @ConfigProperty(name="out.queue.name")
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
    private boolean isLeveragingFailoverProtocol = false;

    private ScheduledExecutorService reconnectScheduler = null;
    private static ObjectMapper mapper = new ObjectMapper();
    private ConnectionFactory connectionFactory;
    private Connection connection = null;
    private  MessageProducer producer;
    private Queue outQueue;
    private Queue inQueue; 
    private MessageConsumer messageConsumer;
    private Session producerSession;
    private Session consumerSession;


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
        if (messageConsumer == null || consumerSession == null) 
            initConsumer();
       
        connection.start();
        logger.info("Connect to broker succeed");
    }

    public boolean isConnected() {
        return connection != null 
            && producerSession != null 
            && consumerSession != null;
    }

    private void initProducer() throws JMSException{
        producerSession = connection.createSession();
        outQueue = producerSession.createQueue(outQueueName);
        producer = producerSession.createProducer(outQueue);
        producer.setTimeToLive(60000); // one minute
    }

    private void initConsumer() throws JMSException {
        consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
        inQueue = consumerSession.createQueue(inQueueName);
        messageConsumer = consumerSession.createConsumer(inQueue);
        messageConsumer.setMessageListener(this);
    }


    void onStart(@Observes StartupEvent ev) {
        logger.info("Started");
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
        disconnect();
    }

    private synchronized void disconnect() {
        closeUtil(messageConsumer);
        closeUtil(consumerSession);
        closeUtil(producerSession);
        closeUtil(producer);
        closeUtil(connection);
        messageConsumer = null;
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

    @Override
    public void onMessage(Message msg) {
        if (msg == null) {
            return;
        }

       TextMessage rawMsg = (TextMessage) msg;
       OrderMessage om;
        try {
            om = mapper.readValue(rawMsg.getText(),OrderMessage.class);
            logger.info("Received message: " + om.toString());
            om.status = OrderMessage.ASSIGNED_STATUS;
            String orderJson= mapper.writeValueAsString(om);
            sendResponse(rawMsg, orderJson);
            msg.acknowledge();
            logger.info("Response sent to replyTo queue " + orderJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch ( JMSException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse( TextMessage rawMsg, String orderJson) throws JMSException{
        if (! isLeveragingFailoverProtocol &&  ! isConnected()) {
            restablishConnection();
        }
        TextMessage outMsg =  producerSession.createTextMessage(orderJson);
        outMsg.setJMSCorrelationID(rawMsg.getJMSCorrelationID());
        producer.send(outMsg);
    }

    @Override
    public void onException(JMSException arg0) {
        logger.error("JMS Exception occurred: " + arg0.getMessage());
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
