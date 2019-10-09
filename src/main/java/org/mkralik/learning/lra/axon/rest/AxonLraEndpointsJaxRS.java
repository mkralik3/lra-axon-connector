package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.eclipse.microprofile.lra.annotation.*;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER;

/**
 * This class is prepared in case you want to use this extension with JAX-RS SPRING-BOOT application instead of
 * SPRING WEB
 *
 * You have to register this endpoint to the service JerseyConfig
 * e.g.
 * register(org.mkralik.learning.lra.axon.rest.AxonLraEndpointsJaxRS.class);
 */
@Service
@Slf4j
@Path("/axonLra")
public class AxonLraEndpointsJaxRS {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;

    @PUT
    @Path("/complete/{aggregateId}")
    public Response complete(@PathParam("aggregateId")  String aggregateId, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector COMPLETE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRACompleteCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.COMPLETE);
    }

    @PUT
    @Path("/compensate/{aggregateId}")
    public Response compensate(@PathParam("aggregateId") String aggregateId, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector COMPENSATE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRACompensateCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.COMPENSATE);
    }

    @GET
    @Path("/status/{aggregateId}")
    public Response status(@PathParam("aggregateId")  String aggregateId, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector STATUS endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAStatusCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.STATUS);
    }

    @DELETE
    @Path("/forget/{aggregateId}")
    public void forget(@PathParam("aggregateId")  String aggregateId, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector FORGET endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAForgetCommand(realAggregateId));
        log.warn("Not implemented yet");
    }

    @PUT
    @Path("/after/{aggregateId}")
    public void after(@PathParam("aggregateId")  String aggregateId, @HeaderParam(LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraEndedId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector AFTER endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAAfterCommand(realAggregateId, lraEndedId));
        log.warn("Not implemented yet");
    }

    @PUT
    @Path("/leave/{aggregateId}")
    public void leave(@PathParam("aggregateId")  String aggregateId, @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector LEAVE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRALeaveCommand(realAggregateId));
        log.warn("Not implemented yet");
    }

    @GET
    @Produces("application/json")
    @Path("/incomingLraContext")
    public Response incomingLraContext() {
        return Response.ok().entity(incomingLraContextsStore.getAllIncomingContext().entrySet()).build();
    }

    @GET
    @Produces("application/json")
    @Path("/aggregateInfo")
    public Response aggregatesInfo() {
        return Response.ok().entity(aggregateTypeInfoStore.getAllAggregatesInfo().entrySet()).build();
    }

    private Response processResult(Object result, String aggregateId, EndpointType type) {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
        if (result instanceof Response) {
            return (Response) result;
        }else if (result==null) {
            // method returns `null` or nothing (void)
            switch (type) {
                case COMPLETE:
                    return builder.entity(ParticipantStatus.Completed.name()).build();
                case COMPENSATE:
                    return builder.entity(ParticipantStatus.Compensated.name()).build();
                case STATUS:
                    throw new IllegalStateException("Status method cannot return null or void");
                default:
                    //afterLra, forget,leave
                    return builder.build();
            }
        }else if(result instanceof ParticipantStatus){
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                return builder.status(Response.Status.ACCEPTED).build();
            } else {
                builder.entity(status.name());
            }
        } else{
            throw new IllegalStateException("not implemented yet");
        }
        return builder.build();
    }

    private enum EndpointType{
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }

}