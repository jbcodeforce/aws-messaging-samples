package org.acme.jms.infra.api;

import org.acme.jms.model.CarRide;

public class CarRideDTO {
   
    public String customerID;
    public String pickup;
    public String destination;
    public String rideDate;
    public String rideTime;
    public int numberOfPassengers;
   
    public CarRideDTO() {
        super();
    }

    public CarRide toCarRide(){
        return new CarRide(customerID, pickup, destination, rideDate, rideTime, numberOfPassengers);
    }
    
}
