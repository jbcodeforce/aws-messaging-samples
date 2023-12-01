package org.acme.jms.infra.api;

import org.acme.jms.infra.msg.CarRidesConsumer;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A simple resource showing the last price.
 */
@Path("/carrides")
public class CarRideMonitoringResource {
    Logger logger = Logger.getLogger(CarRideMonitoringResource.class);
    @Inject
    CarRidesConsumer consumer;

    @GET
    @Path("last")
    @Produces(MediaType.TEXT_PLAIN)
    public String last() {
        logger.info("get last car ride called");
        return consumer.getLastCarRide();
    }
}
