package org.acme.jms.model;
import java.time.LocalDate;

public class CarRide {
    public static final String PENDING_STATUS = "pending";
    public static final String CANCELLED_STATUS = "cancelled";
    public static final String ONHOLD_STATUS = "onHold";
    public static final String ASSIGNED_STATUS = "assigned";
    public static final String COMPLETED_STATUS = "completed";
    static final int SHORT_ID_LENGTH = 8;
    

    public String orderID;
    public String customerID;
    public String pickup;
    public String destination;
    public String status;
    public String creationDate;
    public String updateDate;
    public String rideDate;
    public String rideTime;
    public int numberOfPassengers;
    public double price;

    public CarRide() {
        super();
    }
}
