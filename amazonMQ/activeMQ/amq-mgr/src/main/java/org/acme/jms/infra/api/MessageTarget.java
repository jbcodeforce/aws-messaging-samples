package org.acme.jms.infra.api;

public class MessageTarget {
    public String messageId;
    public String destinationName;
    public String text;

    public MessageTarget() {
        super();
    }

    public MessageTarget(String jmsMessageID, String text) {
        this.messageId = jmsMessageID;
        this.text = text;
    }
}
