package org.acme.jms.infra.api;

public class SimulControl {
    public long delay;
    public int totalMessageToSend;
    public String status;

    public SimulControl() {
        super();
    }

    public String toString() {
        return "SimulControl [delay=" + delay + ", totalMessageToSend=" + totalMessageToSend + ", status=" + status + "]";
    }

}
