package it;


import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.acme.jms.infra.api.QueueDefinition;
import org.acme.jms.infra.msg.QueueBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
@QuarkusTestResource(value = ActiveMQTestResource.class, restrictToAnnotatedClass = true)
public class TestBackend {
   

    static QueueBackend queueBackend;

    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer consumer;
   

    @BeforeAll
    public static void setup() throws JMSException {
    
    }

    @AfterEach
    public void tearDown() throws JMSException {
        // Cleaning up resources
        if (producer != null) producer.close();
        if (consumer != null) consumer.close();
        if (session != null) session.close();
        if (connection != null) connection.close();
    }

    @Test
    public void shouldCreateAQueueAListIt(){
        
        QueueDefinition carQueue = new QueueDefinition("CarRides");
        queueBackend = new QueueBackend();
        queueBackend.connectionURLs=System.getProperty("ACTIVEMQ_URL");
        Assertions.assertTrue(queueBackend.createQueue(carQueue));
        List<QueueDefinition> l = queueBackend.listQueues();
        Assertions.assertNotNull(l);
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals("CarRides", l.get(0).name);
    } 
}
