package org.acme.jms.infra.api;

public class QueueDefinition {
   
    public String name;
    public boolean persistent = true;
   
    public QueueDefinition() {
        super();
    }

    public QueueDefinition(String queueName) {
        name=queueName;
    }

    public String toString(){
        return "{ \"queue\" : " + name + "}";
    }
    
}
