package org.acme.jms;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
/**
 * A bean producing random prices every n seconds and sending them to the prices JMS queue.
 */
@ApplicationScoped
public class PriceProducer implements Runnable {
    Logger logger = Logger.getLogger("PriceProducer");
    
    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    @ConfigProperty(name="queue.name")
    public String queueName;

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
            context.createProducer().send(context.createQueue(queueName), 
                    Integer.toString(random.nextInt(100)));
        }
    }

    public void sendNmessages(int totalMessageToSend) throws InterruptedException {
        logger.info("Sending " + totalMessageToSend + " messages");
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < totalMessageToSend; i++) {
                producer.send(context.createQueue("prices"), 
                    Integer.toString(random.nextInt(100)));
                Thread.sleep(500);
            }
        }
    }
}
