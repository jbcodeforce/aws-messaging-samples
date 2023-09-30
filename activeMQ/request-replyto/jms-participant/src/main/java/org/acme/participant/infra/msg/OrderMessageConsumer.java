package org.acme.participant.infra.msg;

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
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * A OrderMessage Consumer to participate to the order process asynchrnously
 */
@ApplicationScoped
public class OrderMessageConsumer implements MessageListener {
    Logger logger = Logger.getLogger(OrderMessageConsumer.class.getName());
    
    @Inject
    @ConfigProperty(name="in.queue.name")
    public String inQueueName;
    @Inject
    @ConfigProperty(name="out.queue.name")
    public String outQueueName;
    @Inject
    @ConfigProperty(name="quarkus.artemis.url")
    public String connectionURLs;
    @Inject
    @ConfigProperty(name="quarkus.artemis.username")
    private String user;

    @Inject
    @ConfigProperty(name="quarkus.artemis.password")
    private String password;

    private static ObjectMapper mapper = new ObjectMapper();
    private ConnectionFactory connectionFactory;
    private Connection connection = null;
    private  MessageProducer producer;
    private MessageConsumer consumer;
    private Queue outQueue;
    private Queue inQueue; 
    private MessageConsumer messageConsumer;
    private Session producerSession;
    private Session consumerSession;


    
    
    private void init() throws JMSException {
        connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
        connection = connectionFactory.createConnection(user, password);
        connection.setClientID("p-" + System.currentTimeMillis());

        producerSession = connection.createSession();
        
        outQueue = producerSession.createQueue(outQueueName);
        producer = producerSession.createProducer(outQueue);

        consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
        inQueue = consumerSession.createQueue(inQueueName);
        
        messageConsumer = consumerSession.createConsumer(inQueue);
        messageConsumer.setMessageListener(this);
        connection.start();
          
    }

    void onStart(@Observes StartupEvent ev) {
        logger.info("Started");
        try {
            init();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (connection != null)
			try {
				connection.close();
			} catch (JMSException e) {
				e.printStackTrace();
			};
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
        TextMessage outMsg =  producerSession.createTextMessage(orderJson);
        outMsg.setJMSCorrelationID(rawMsg.getJMSCorrelationID());
        producer.send(outMsg);
        msg.acknowledge();
        logger.info("Reponse sent to replyTo queue " + orderJson);
    } catch (JsonProcessingException | JMSException e) {
        e.printStackTrace();
    }
       

    }

}
