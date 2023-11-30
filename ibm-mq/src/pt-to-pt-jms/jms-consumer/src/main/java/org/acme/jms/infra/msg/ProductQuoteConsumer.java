package org.acme.jms.infra.msg;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.acme.jms.model.Quote;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;


/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class ProductQuoteConsumer implements MessageListener, ExceptionListener{
    Logger logger = Logger.getLogger(ProductQuoteConsumer.class.getName());
    
    @Inject
    @ConfigProperty(name="reconnect.delay.ins")
    public int reconnectDelay;

    @Inject
    @ConfigProperty(name = "mq.host", defaultValue = "localhost" )
    public String mqHostname;

    @Inject
    @ConfigProperty(name = "mq.port", defaultValue = "1414")
    public int mqHostport;

    @Inject
    @ConfigProperty(name = "mq.qmgr", defaultValue = "QM1")
    public String mqQmgr;

    @Inject
    @ConfigProperty(name = "mq.channel", defaultValue = "DEV.APP.SVRCONN")
    public String mqChannel;

    @Inject
    @ConfigProperty(name = "mq.app_user", defaultValue = "admin")
    public String mqAppUser;

    @Inject
    @ConfigProperty(name = "mq.app_password", defaultValue = "passw0rd")
    public String mqPassword;

    @Inject
    @ConfigProperty(name = "mq.queue_name", defaultValue = "DEV.QUEUE.1")
    public String queueName;

    @Inject
    @ConfigProperty(name = "app.name", defaultValue = "TestApp")
    public String appName;

    @Inject
    @ConfigProperty(name = "mq.cipher_suite")
    public Optional<String> mqCipherSuite;

    @Inject
    @ConfigProperty(name = "mq.ccdt_url")
    public Optional<String> mqCcdtUrl;

    JmsConnectionFactory connectionFactory;
    private Connection connection = null;
    private MessageConsumer messageConsumer;
    private Session consumerSession;
    private Destination inQueue;
    private JMSContext jmsContext = null;
    private static ObjectMapper mapper = new ObjectMapper();
    private ScheduledExecutorService reconnectScheduler = null;
    private volatile String lastPrice;

    public String getLastQuote() {
        return lastPrice;
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
            JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            connectionFactory = ff.createConnectionFactory();
            String ccdtFilePath = validateCcdtFile();
            if (ccdtFilePath == null) {
                logger.info("No valid CCDT file detected. Using host, port, and channel properties instead.");
                connectionFactory.setStringProperty(WMQConstants.WMQ_HOST_NAME, this.mqHostname);
                connectionFactory.setIntProperty(WMQConstants.WMQ_PORT, this.mqHostport);
                connectionFactory.setStringProperty(WMQConstants.WMQ_CHANNEL, this.mqChannel);
            } else {
                logger.info("Setting CCDTURL to 'file://" + ccdtFilePath + "'");
                connectionFactory.setStringProperty(WMQConstants.WMQ_CCDTURL, "file://" + ccdtFilePath);
            }
            if (this.mqCipherSuite != null && !("".equalsIgnoreCase(this.mqCipherSuite.orElse("")))) {
                connectionFactory.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, this.mqCipherSuite.orElse(""));
            }
            connectionFactory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            connectionFactory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, this.mqQmgr);
            connectionFactory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, this.appName);
            connectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, false);
            connectionFactory.setStringProperty(WMQConstants.USERID, this.mqAppUser);
            connectionFactory.setStringProperty(WMQConstants.PASSWORD, this.mqPassword);
           
            connection = connectionFactory.createConnection(mqAppUser, mqPassword);
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
      
        }
        if (messageConsumer == null || consumerSession == null) {
            jmsContext = connectionFactory.createContext();
            inQueue = jmsContext.createQueue("queue:///" + this.queueName);
            consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
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
       Quote om;
        try {
            om = mapper.readValue(rawMsg.getText(),Quote.class);
            logger.info("Received message: " + om.toString());
            lastPrice = rawMsg.getText();
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
        logger.info("Hostname: " + mqHostname);
        logger.info("Port: " + mqHostport);
        logger.info("Channel: " + mqChannel);
        logger.info("Qmgr: " + mqQmgr);
        logger.info("App User: " + mqAppUser);
        logger.debug("App Password: " + mqPassword);
    }

    private String validateCcdtFile() {
        /*
        * Modeled after
        * github.com/ibm-messaging/mq-dev-patterns/blob/master/JMS/com/ibm/mq/samples/
        * jms/SampleEnvSetter.java
        */
        String value = mqCcdtUrl.orElse("");
        String filePath = null;
        if (value != null && !value.isEmpty()) {
        logger.info("Checking for existence of file " + value);
        File tmp = new File(value);
        if (!tmp.exists()) {
            logger.info(value + " does not exist...");
            filePath = null;
        } else {
            logger.info(value + " exists!");
            filePath = value;
        }
        }
    return filePath;
  }

 
}
