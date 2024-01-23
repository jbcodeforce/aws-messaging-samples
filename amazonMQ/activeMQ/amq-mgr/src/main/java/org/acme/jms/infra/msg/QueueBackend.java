package org.acme.jms.infra.msg;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.acme.jms.infra.api.MessageTarget;
import org.acme.jms.infra.api.QueueDefinition;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.advisory.DestinationSource;
import org.apache.activemq.command.ActiveMQQueue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;


/**
 * The Active MQ backend wrapper to support the exposed REST API.
 */
@ApplicationScoped
public class QueueBackend implements ExceptionListener {
    Logger logger = Logger.getLogger(QueueBackend.class.getName());
    
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
    @ConfigProperty(name = "app.name", defaultValue = "amq-mgr")
    public String appName;


    private ActiveMQConnectionFactory connectionFactory;
    private ActiveMQConnection connection = null;

    private ScheduledExecutorService reconnectScheduler = null;
    private MessageProducer producer = null;
    

  

    void onStop(@Observes ShutdownEvent ev) {
        if (reconnectScheduler != null)
            reconnectScheduler.shutdownNow();
        disconnect();
    }

    private synchronized void disconnect() {
        logger.info("Disconnecting");
        closeUtil(connection);
        connection = null;
    }

    private synchronized void restablishConnection() throws JMSException {
        if (! isConnected()) {
            displayParameters();
            connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
            connection = (ActiveMQConnection) connectionFactory.createConnection();
            connection.setClientID("p-" + System.currentTimeMillis());
            connection.setExceptionListener(this);
        } 
        connection.start();
        logger.info("Connect to broker succeed");
    }

    public boolean isConnected() {
        return connection != null;
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
    logger.info("App User: " +  user);
    logger.debug("App Password: " + password);
  }

    @Override
    public void onException(JMSException arg0) {
        logger.error("JMS Exception occurred: " + arg0.getMessage());
        disconnect();
        reconnect(reconnectDelay);
    }

    public List<QueueDefinition> listQueues() {
        List<QueueDefinition> response = new ArrayList<QueueDefinition>();
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            DestinationSource ds = connection.getDestinationSource();
            Set<ActiveMQQueue> queues = ds.getQueues();

            for(ActiveMQQueue queue : queues){
                QueueDefinition qd = new QueueDefinition(queue.getQueueName());
                response.add(qd);
            }
            disconnect();
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        
        return response;
    }

    public boolean createQueue(QueueDefinition definition) {
        logger.info("Create a new queue " + definition.toString());
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            Session session=connection.createSession(true,Session.SESSION_TRANSACTED);
            Destination destination = session.createQueue(definition.name);
            MessageProducer producer = session.createProducer(destination);
            if (definition.persistent)
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            else 
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            TextMessage message = session.createTextMessage("queue-created");
            message.setJMSMessageID(UUID.randomUUID().toString().substring(0,10));
            producer.send(message);
            session.commit();
            session.close();
           
        } catch (JMSException e) {
            e.printStackTrace();
            return false;
        } finally {
            disconnect();
        }
        return true;
    }

    public void deleteQueue(QueueDefinition definition) {
        logger.info("Delete a queue " + definition.toString());
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            connection.destroyDestination(new ActiveMQQueue(definition.name));
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public boolean moveMessageToDestination(String queue_name, MessageTarget definition) {
        logger.info("Move message: " + definition.messageId + " from " + queue_name);
        Message fromMsg = null;
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            Session consumerSession=connection.createSession(true,Session.AUTO_ACKNOWLEDGE);
            Queue inQueue = consumerSession.createQueue(queue_name);
            MessageConsumer consumer = consumerSession.createConsumer(inQueue,"JMSMessageID='"+definition.messageId+"'");
            fromMsg = consumer.receive(1000);
            if (fromMsg != null) {
                Session producerSession = connection.createSession(true,Session.SESSION_TRANSACTED);
                Destination destination = producerSession.createQueue(definition.destinationName);
                producer = producerSession.createProducer(destination);
                producer.send(fromMsg);
                producerSession.commit();
                fromMsg.acknowledge();
                producerSession.close();
            }
            consumerSession.close();
           
        } catch (JMSException e) {
            e.printStackTrace();
            disconnect();
            return false;
        } finally {
            disconnect();
        }
        return true;
      }


    public void sendTextMessageToDestination(String destinationName, String txtMessage){
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            Session session = connection.createSession(true,Session.SESSION_TRANSACTED);
            Destination destination = session.createQueue(destinationName);
            producer = session.createProducer(destination);

            TextMessage message =  session.createTextMessage(txtMessage);
            message.setJMSMessageID(UUID.randomUUID().toString());
            producer.send(message);
            session.commit();
            session.close();
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }  
        
    }

    public List<MessageTarget> browseQueue(String queue_name) {
        List<MessageTarget> messages = new ArrayList<MessageTarget>();
        try {
            if (! isConnected()) {
                restablishConnection();
            }
            Session session = connection.createSession();
            Queue destination = session.createQueue(queue_name);
            QueueBrowser browser = session.createBrowser(destination);
            Enumeration v = browser.getEnumeration();
            while (v.hasMoreElements()) {
                TextMessage msg = (TextMessage)v.nextElement();
                MessageTarget mt = new MessageTarget(msg.getJMSMessageID(),msg.getText());
                messages.add(mt);
            }
               
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
       return messages;
    }
}
