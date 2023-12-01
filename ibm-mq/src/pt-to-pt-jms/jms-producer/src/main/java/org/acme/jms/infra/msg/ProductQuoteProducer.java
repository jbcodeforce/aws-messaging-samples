package org.acme.jms.infra.msg;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
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
 * A bean producing random prices every n seconds and sending them to the prices JMS queue.
 */
@ApplicationScoped
public class ProductQuoteProducer implements Runnable, ExceptionListener {
    Logger logger = Logger.getLogger(ProductQuoteProducer.class.getName());
    
   
 
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
    private MessageProducer producer = null;
    private Session producerSession;
    private Destination outQueue;
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
        if (producer == null || producerSession == null) {
            jmsContext = connectionFactory.createContext();
            outQueue = jmsContext.createQueue("queue:///" + this.queueName);
            producerSession = connection.createSession();
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
        try {
           
            Quote q = Quote.createRandomQuote();
            String quoteJson= mapper.writeValueAsString(q);
            TextMessage msg =  jmsContext.createTextMessage(quoteJson);
            msg.setJMSMessageID(UUID.randomUUID().toString());
            producer.send(msg);
            logger.info("Sent: " + quoteJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendNmessages(int totalMessageToSend) throws InterruptedException {
        logger.info("Sending " + totalMessageToSend + " messages");
        
        for (int i = 0; i < totalMessageToSend; i++) {
                run();
                Thread.sleep(500);
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

  private void displayParameters() {

    logger.info("##########  Connection parameters #######");
    logger.info("Hostname: " + mqHostname);
    logger.info("Port: " + mqHostport);
    logger.info("Channel: " + mqChannel);
    logger.info("Qmgr: " + mqQmgr);
    logger.info("App User: " + mqAppUser);
    logger.debug("App Password: " + mqPassword);
  }
}
