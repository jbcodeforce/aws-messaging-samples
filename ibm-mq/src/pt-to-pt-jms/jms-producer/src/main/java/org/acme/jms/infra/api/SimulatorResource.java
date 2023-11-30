package org.acme.jms.infra.api;

import org.acme.jms.infra.msg.ProductQuoteProducer;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/simulator")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimulatorResource {
    Logger logger = Logger.getLogger("SimulatorResource");
    
    @Inject
    ProductQuoteProducer producer;

    @POST
    public SimulControl startSimulator(SimulControl control) {
        logger.info("Received control: " + control);
        if (control == null) throw 
            new WebApplicationException("Control should not be empty");
        
        if (control.delay >0 ) {
            producer.start(control.delay);
        } else if (control.totalMessageToSend > 0) {
            try {
                producer.sendNmessages(control.totalMessageToSend);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        control.status="Started";
        return control;         
    }
}
