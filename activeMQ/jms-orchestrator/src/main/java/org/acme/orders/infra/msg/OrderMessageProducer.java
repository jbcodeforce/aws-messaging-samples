package org.acme.orders.infra.msg;


import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.acme.orders.domain.Order;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
/**
 * OrderMessageProcessing is a producer to the orders queue and consumer on replyTo queue
 */
@ApplicationScoped
public class OrderMessageProducer implements Runnable {
    Logger logger = Logger.getLogger(OrderMessageProducer.class.getName());
    
    
    ConnectionFactory connectionFactory;

    @Inject
    @ConfigProperty(name="main.queue.name")
    public String mainQueueName;

    @Inject
    @ConfigProperty(name="replyTo.queue.name")
    public String replyToQueueName;

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
    private ScheduledExecutorService scheduler = null;
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
    public void init() throws JMSException {
        connectionFactory = new ActiveMQConnectionFactory(connectionURLs);
        connection = connectionFactory.createConnection(user, password);
        connection.setClientID("j-" + System.currentTimeMillis());

        producerSession = connection.createSession();
        
        mainQueue = producerSession.createQueue(mainQueueName);
        producer = producerSession.createProducer(mainQueue);

        consumerSession = connection.createSession(true,Session.CLIENT_ACKNOWLEDGE);
        replyToQueue = consumerSession.createQueue(replyToQueueName);
        
        messageConsumer = consumerSession.createConsumer(replyToQueue);
        
        connection.start();
          
    }

    /**
     * Use following code to start automatically when app starts 
    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }
`*/
    
void onStop(@Observes ShutdownEvent ev) {
        if (scheduler != null) 
            scheduler.shutdownNow();
        if (connection != null)
			try {
				connection.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
    }
    

    public void start(long delay) {
        if (scheduler == null) 
            scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this, 0L, delay, TimeUnit.SECONDS);
    }


    @Override
    public void run() {
        Order o = Order.buildOrder();
        sendMessage(o);
    }

    public void sendMessage(Order order) {
       
        try {
            if ( connection == null) {
                init();
            }
            OrderMessage oe = OrderMessage.fromOrder(order);
            String orderJson= mapper.writeValueAsString(oe);
            TextMessage msg =  producerSession.createTextMessage(orderJson);
            msg.setJMSMessageID(UUID.randomUUID().toString());
            producer.send( msg);  
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        
    }
}
