package org.acme.orders;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSProducer;
import jakarta.jms.MessageProducer;
import jakarta.jms.TextMessage;
/**
 * A bean producing random prices every n seconds and sending them to the prices JMS queue.
 */
@ApplicationScoped
public class ProductQuoteProducer implements Runnable {
    Logger logger = Logger.getLogger(ProductQuoteProducer.class.getName());
    
    public static String[] skus = {"sku1", "sku2", "sku3", "sku4", "sku5", "sku6", "sku7", "sku8", "sku9", "sku10"};
    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    @ConfigProperty(name="queue.name")
    public String queueName;

    @Inject
    @ConfigProperty(name="quarkus.artemis.url")
    public String connectionURLs;

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void init() {

    }

    /**
     * Use following code to start automatically when app starts 
    void onStart(@Observes StartupEvent ev) {
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }
    */ 

    void start(long delay) {
        scheduler.scheduleWithFixedDelay(this, 0L, delay, TimeUnit.SECONDS);
    }

    void stop() {
        scheduler.shutdown();
    }


    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
           
            Order q = new Order();
            TextMessage msg =  context.createTextMessage(q.toString());
            msg.setJMSMessageID(UUID.randomUUID().toString());
            JMSProducer producer = context.createProducer();
            producer.send(context.createQueue(queueName),msg);
            logger.info("Sent: " + q.toString());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendNmessages(int totalMessageToSend) throws InterruptedException {
        logger.info("Sending " + totalMessageToSend + " messages");
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < totalMessageToSend; i++) {
                Order q = createRandomQuote();
                TextMessage msg =  context.createTextMessage(q.toString());
                msg.setJMSMessageID(UUID.randomUUID().toString());
                producer.send(context.createQueue(queueName), msg);
                logger.info("Sent: " + q.toString());
                Thread.sleep(500);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
