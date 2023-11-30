package org.acme.jms.infra.api;

import org.acme.jms.infra.msg.ProductQuoteConsumer;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A simple resource showing the last price.
 */
@Path("/prices")
public class QuoteResource {
    Logger logger = Logger.getLogger(QuoteResource.class);
    @Inject
    ProductQuoteConsumer consumer;

    @GET
    @Path("last")
    @Produces(MediaType.TEXT_PLAIN)
    public String last() {
        logger.info("get last quote called");
        return consumer.getLastQuote();
    }
}
