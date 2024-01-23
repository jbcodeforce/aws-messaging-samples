package org.acme.jms.infra.api;

import org.acme.jms.infra.msg.QueueBackend;
import org.acme.jms.model.CarRide;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Define operations on the Queue resources
 */
@ApplicationScoped
@Path("/carrides")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarRideResource {
    Logger logger = Logger.getLogger("QueueResource");
    @Inject
    QueueBackend queueBackend;
    private static ObjectMapper mapper = new ObjectMapper();
    @POST
    @Path("/{queue_name}/")
    public Response writeMessageToTheQueue(String queue_name, CarRide carRide){
        String rideJson;
        try {
            rideJson = mapper.writeValueAsString(carRide);
            queueBackend.sendTextMessageToDestination(queue_name,rideJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        
        return Response.ok().build();
    }
}
