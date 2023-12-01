package org.acme.participant.infra.msg;

public class OrderMessage {
    public static final String ASSIGNED_STATUS = "assigned";
    public static final String REJECTED_STATUS = "rejected";
    
    public String messageID;
    public String orderID;
    public String sku;
    public double price;
    public int quantity;
    public String status;

    public OrderMessage() {
    }

    public String toString(){
        return messageID + "," + orderID + "," + sku + "," + price + "," + quantity + "," + status;
    }
}
