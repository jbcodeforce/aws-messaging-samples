package org.acme.jms.model;
import java.time.LocalDate;

import org.apache.commons.lang3.RandomStringUtils;
import java.util.Random;


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

    public CarRide(String customerID2, String pickup2, String destination2, String rideDate2, String rideTime2,
            int numberOfPassengers2) {
            this.customerID= customerID2;
            this.pickup= pickup2;
            this.destination= destination2;
            this.rideDate= rideDate2;
            this.rideTime= rideTime2;
            this.numberOfPassengers= numberOfPassengers2;
            this.creationDate = LocalDate.now().toString();
            this.updateDate = this.creationDate;
            this.status= PENDING_STATUS;
            this.orderID = "CarRide_" + RandomStringUtils.randomAlphabetic(SHORT_ID_LENGTH);
    }

    public static CarRide createRandomQRide(){
        Random random = new Random();
        CarRide cr =  new CarRide();
        cr.orderID = "CarRide_" + RandomStringUtils.randomAlphabetic(SHORT_ID_LENGTH);
        cr.customerID = "C_" + RandomStringUtils.randomAlphabetic(SHORT_ID_LENGTH);
        cr.pickup= "Location_" + random.nextInt(10);
        cr.destination= "Location_" + random.nextInt(10);
        cr.status= PENDING_STATUS;
        cr.creationDate= LocalDate.now().toString();
		cr.updateDate= cr.creationDate;
        cr.rideDate= LocalDate.now().toString();
        cr.numberOfPassengers= random.nextInt(4);
        return cr;
    }
}
