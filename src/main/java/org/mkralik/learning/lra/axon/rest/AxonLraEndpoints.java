package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.eclipse.microprofile.lra.annotation.*;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Service
@Slf4j
@Path("/axonLra")
public class AxonLraEndpoints {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    @PUT
    @Path("/complete/{id}")
    public Response complete(@PathParam("id")  String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector COMPLETE endpoint id: {}", id);
        return processResult(commandGateway.sendAndWait(new LraCompleteCommand(realId)), EndpointType.COMPLETE);
    }

    @PUT
    @Path("/compensate/{id}")
    public Response compensate(@PathParam("id") String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector COMPENSATE endpoint id: {}", id);
        return processResult(commandGateway.sendAndWait(new LraCompensateCommand(id)),EndpointType.COMPENSATE);
    }

    @GET
    @Path("/status/{id}")
    public Response status(@PathParam("id")  String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector STATUS endpoint id: {}", id);
        return processResult(commandGateway.sendAndWait(new LraStatusCommand(id)), EndpointType.STATUS);
    }

    @DELETE
    @Path("/forget/{id}")
    public void forget(@PathParam("id")  String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector FORGET endpoint id: {}", id);
        commandGateway.sendAndWait(new LraForgetCommand(id));
        log.warn("Not implemented");
    }

    @PUT
    @Path("/after/{id}")
    public void after(@PathParam("id")  String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector AFTER endpoint id: {}", id);
        commandGateway.sendAndWait(new LraAfterCommand(id));
        log.warn("Not implemented");
    }

    @PUT
    @Path("/leave/{id}")
    public void leave(@PathParam("id")  String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXON LRA connector AFTER endpoint id: {}", id);
        commandGateway.sendAndWait(new LraLeaveCommand(id));
        log.warn("Not implemented");
    }

    @GET
    @Path("/incomingLraContext")
    public Response incomingLraContext() {
        return Response.ok().entity(incomingLraContextsStore.getAllIncomingContext().entrySet()).build();
    }

    private Response processResult(Object result, EndpointType type) {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
        if (result==null) {
            // void return type and no exception was thrown
            builder.entity(type.equals(EndpointType.COMPLETE) ? ParticipantStatus.Completed.name() : ParticipantStatus.Compensated.name());
            return builder.build();
        } else if(result instanceof ParticipantStatus){
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                return builder.status(Response.Status.ACCEPTED).build();
            } else {
                builder.entity(status.name());
            }
        } else if (result instanceof Response) {
            return (Response) result;
        } else{
            throw new IllegalStateException("not implemented yet");
        }
        return builder.build();
    }

    private enum EndpointType{
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }

}
