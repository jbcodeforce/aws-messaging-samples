package org.acme.orders.infra.api;

import java.util.List;

import org.acme.orders.domain.Order;
import org.acme.orders.domain.OrderService;
import org.acme.orders.infra.repo.OrderRepository;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {
    Logger logger = Logger.getLogger(OrderResource.class.getName());
    
    @Inject
    OrderService service;

    @Inject
    OrderRepository orderRepository;

    @GET
    public List<Order> getOrders(){
        return orderRepository.getAllOrders();
    } 

    @GET
    @Path("/{id}")
    public Order getOrderById(@PathParam("id") String id){
        return orderRepository.findById(id);
    }

    @POST
    public Order SaveOrder(Order newOrder){
        if (newOrder == null) throw 
            new WebApplicationException("Order should not be empty");
        return service.processOrder(newOrder);
    }

    @POST
    @Path("/simulation")
    public SimulControl startSimulator(SimulControl control) {
        logger.info("Received control: " + control);
        if (control == null) throw 
            new WebApplicationException("Control should not be empty");
        service.startOrderSimulation(control);
       
        control.status="Started";
        return control;         
    }
}
