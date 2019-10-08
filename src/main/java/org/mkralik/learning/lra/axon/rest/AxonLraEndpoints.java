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
    @Path("/complete/{aggregateId}")
    public Response complete(@PathParam("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector COMPLETE endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRACompleteCommand(realId)), EndpointType.COMPLETE);
    }

    @PUT
    @Path("/compensate/{aggregateId}")
    public Response compensate(@PathParam("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector COMPENSATE endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRACompensateCommand(realId)), EndpointType.COMPENSATE);
    }

    @GET
    @Path("/status/{aggregateId}")
    public Response status(@PathParam("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector STATUS endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRAStatusCommand(realId)), EndpointType.STATUS);
    }

    @DELETE
    @Path("/forget/{aggregateId}")
    public void forget(@PathParam("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector FORGET endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRAForgetCommand(realId));
        log.warn("Not implemented");
    }

    @PUT
    @Path("/after/{aggregateId}")
    public void after(@PathParam("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector AFTER endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRAAfterCommand(realId));
        log.warn("Not implemented");
    }

    @PUT
    @Path("/leave/{aggregateId}")
    public void leave(@PathParam("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector LEAVE endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRALeaveCommand(realId));
        log.warn("Not implemented");
    }

    @GET
    @Path("/incomingLraContext")
    public Response incomingLraContext() {
        return Response.ok().entity(incomingLraContextsStore.getAllIncomingContext().entrySet()).build();
    }

    private Response processResult(Object result, EndpointType type) {
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
