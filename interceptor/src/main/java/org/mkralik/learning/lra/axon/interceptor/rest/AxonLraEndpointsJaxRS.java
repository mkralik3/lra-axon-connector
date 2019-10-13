package org.mkralik.learning.lra.axon.interceptor.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.*;

/**
 * This class is prepared in case the user wants to use this extension with SPRING-BOOT application which uses JAX-RS.
 * After register this clas to the service's config file JerseyConfig, these endpoints are available for coordinator.
 * <p>
 * e.g.
 * register(org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsJaxRS.class);
 */
@Service
@Slf4j
@Path("/axonLra")
public class AxonLraEndpointsJaxRS {

    @Autowired
    private AxonLraEndpointsSpring springRestEndpoint;

    @PUT
    @Path("/complete/{aggregateId}")
    public Response complete(@PathParam("aggregateId") String aggregateId,
                             @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                             @HeaderParam(LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentLraId) throws Exception {
        log.debug("AXON LRA connector COMPLETE JAX-RS endpoint. Redirect to spring based endpoint");
        return convertSpringResponseEntityToResponse(springRestEndpoint.complete(aggregateId, lraId, parentLraId));
    }

    @PUT
    @Path("/compensate/{aggregateId}")
    public Response compensate(@PathParam("aggregateId") String aggregateId,
                               @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                               @HeaderParam(LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentLraId) throws Exception {
        log.debug("AXON LRA connector COMPENSATE JAX-RS endpoint. Redirect to spring based endpoint");
        return convertSpringResponseEntityToResponse(springRestEndpoint.compensate(aggregateId, lraId, parentLraId));
    }

    @GET
    @Path("/status/{aggregateId}")
    public Response status(@PathParam("aggregateId") String aggregateId,
                           @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                           @HeaderParam(LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentLraId) throws Exception {
        log.debug("AXON LRA connector STATUS JAX-RS endpoint. Redirect to spring based endpoint");
        return convertSpringResponseEntityToResponse(springRestEndpoint.status(aggregateId, lraId, parentLraId));
    }

    @DELETE
    @Path("/forget/{aggregateId}")
    public Response forget(@PathParam("aggregateId") String aggregateId,
                           @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                           @HeaderParam(LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentLraId) throws Exception {
        log.debug("AXON LRA connector FORGET JAX-RS endpoint. Redirect to spring based endpoint");
        return convertSpringResponseEntityToResponse(springRestEndpoint.forget(aggregateId, lraId, parentLraId));
    }

    @PUT
    @Path("/after/{aggregateId}")
    public Response after(@PathParam("aggregateId") String aggregateId,
                          @HeaderParam(LRA_HTTP_ENDED_CONTEXT_HEADER) URI endedLraId,
                          String lraStatus) throws Exception {
        log.debug("AXON LRA connector AFTER JAX-RS endpoint. Redirect to spring based endpoint");
        return convertSpringResponseEntityToResponse(springRestEndpoint.after(aggregateId, endedLraId, lraStatus));
    }

    @PUT
    @Path("/leave/{aggregateId}")
    public void leave(@PathParam("aggregateId") String aggregateId,
                      @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws Exception {
        log.debug("AXON LRA connector LEAVE JAX-RS endpoint. Redirect to spring based endpoint");
        springRestEndpoint.leave(aggregateId, lraId);
    }

    @GET
    @Produces("application/json")
    @Path("/incomingLraContext")
    public Response incomingLraContext() {
        return convertSpringResponseEntityToResponse(springRestEndpoint.incomingLraContext());
    }

    @GET
    @Produces("application/json")
    @Path("/aggregateInfo")
    public Response aggregatesInfo() {
        return convertSpringResponseEntityToResponse(springRestEndpoint.aggregatesInfo());
    }

    private Response convertSpringResponseEntityToResponse(ResponseEntity re) {
        Response.ResponseBuilder builder = Response.status(re.getStatusCode().value());
        builder.entity(re.getBody());
        return builder.build();
    }
}