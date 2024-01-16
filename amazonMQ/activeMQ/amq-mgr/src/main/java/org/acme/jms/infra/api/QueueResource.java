package org.acme.jms.infra.api;

import java.util.List;

import org.acme.jms.infra.msg.QueueBackend;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Define operations on the Queue resources
 */
@ApplicationScoped
@Path("/queues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QueueResource {
    Logger logger = Logger.getLogger("QueueResource");
    @Inject
    QueueBackend queueBackend;

    @POST
    public Response createQueue(QueueDefinition definition) {
        logger.info("Queue definition: " + definition.toString());
        if (queueBackend.createQueue(definition)) {
            return Response.status(Status.CREATED).build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }                
    }

    @GET
    public List<QueueDefinition> getExistingQueues() {
        return queueBackend.listQueues();
    }


    @DELETE
    public Response deleteQueue(QueueDefinition definition) {
        queueBackend.deleteQueue(definition);
        return Response.ok().build(); 
    }

    @PUT
    @Path("/{queue_name}/moveMessageTo")
    public Response moveMessageToDestination(String queue_name, MessageTarget definition) {
        if (queueBackend.moveMessageToDestination(queue_name, definition)) {
            return Response.ok().build();
        } else {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
         
    }

}
