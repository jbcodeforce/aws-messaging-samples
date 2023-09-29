package org.acme.jms;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class ProductQuoteConsumer implements Runnable {
    Logger logger = Logger.getLogger(ProductQuoteConsumer.class.getName());
    
    @Inject
    ConnectionFactory connectionFactory;

    @Inject
    @ConfigProperty(name="queue.name")
    public String queueName;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile String lastPrice;

    public String getLastQuote() {
        return lastPrice;
    }

    void onStart(@Observes StartupEvent ev) {
        logger.info("Started");
        scheduler.scheduleWithFixedDelay(this, 0L, 5L, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        logger.info("Start runnable");
        try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
            logger.info("Created context");
            JMSConsumer consumer = context.createConsumer(context.createQueue(queueName));
            logger.info("Started listening to JMS");
            while (true) {
                Message message = consumer.receive();
                if (message == null) {
                    // receive returns `null` if the JMSConsumer is closed
                    return;
                }
                lastPrice = message.getBody(String.class);
                logger.info("Got last quote: " + lastPrice);
            }
        } catch (JMSException e) {
            logger.info("Stopped runner");
            throw new RuntimeException(e);
        }
       
    }
}
