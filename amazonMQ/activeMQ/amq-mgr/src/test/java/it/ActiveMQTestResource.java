package it;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ActiveMQTestResource implements QuarkusTestResourceLifecycleManager{

    public static GenericContainer<?> activeMQContainer;

    @Override
    public Map<String, String> start() {
       
        activeMQContainer = new GenericContainer<>(DockerImageName.parse("apache/activemq-classic:5.17.6"))
                    .withExposedPorts(61616);
        activeMQContainer.start();
        String brokerUrl = "tcp://localhost:" + activeMQContainer.getMappedPort(61616);
        Map<String, String> conf = new HashMap<>();
        conf.put("activemq.url",brokerUrl);
        System.setProperty("ACTIVEMQ_URL", brokerUrl);
        return conf;
    }

    @Override
    public void stop() {
        activeMQContainer.stop();
    }
    
}
