package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.mkralik.learning.lra.axon.api.AxonLraCompensateCommand;
import org.mkralik.learning.lra.axon.api.AxonLraCompleteCommand;
import org.mkralik.learning.lra.axon.store.LraContextsStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@RestController
@Slf4j
@RequestMapping("/axonLra")
public class AxonLraEndpointsSpring {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private LraContextsStore lraContextsStore;

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    @PutMapping("/complete/{id}")
    public Response complete(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function complete with id: {}", id);
        return processResult(commandGateway.sendAndWait(new AxonLraCompleteCommand(realId)), EndpointType.COMPLETE);
    }

    @PutMapping("/compensate/{id}")
    public Response compensate(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function compensate with id: {}", realId);
        return processResult(commandGateway.sendAndWait(new AxonLraCompensateCommand(id)),EndpointType.COMPENSATE);
    }

    @GetMapping("/status/{id}")
    public void status(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function status with id: {}", id);
        log.warn("Not implemented");
    }

    @DeleteMapping("/forget/{id}")
    public void forget(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function forget with id: {}", id);
        log.warn("Not implemented");
    }

    @PutMapping("/after/{id}")
    public void after(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function after with id: {}", id);
        log.warn("Not implemented");
    }

    @PutMapping("/leave/{id}")
    public void leave(@PathVariable String id) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( id, "UTF-8" );
        log.info("in the AXONLRA endpoint function leave with id: {}", id);
        log.warn("Not implemented");
    }

    @GetMapping("/incomingLraContext")
    public Response incomingLraContext() {
        return Response.ok().entity(incomingLraContextsStore.getAllIncomingContext().entrySet()).build();
    }

    @GetMapping("/actualLraContext")
    public Response actualLraContext() {
        return Response.ok().entity(lraContextsStore.getAllContext().entrySet()).build();
    }

    private Response processResult(Object result, EndpointType type) {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
        if (result==null) {
            // void return type and no exception was thrown
            builder.entity(type.equals(EndpointType.COMPLETE) ? ParticipantStatus.Completed.name() : ParticipantStatus.Compensated.name());
            return builder.build();
        }else{
            throw new IllegalStateException("not implemented yet");
        }
    }

    private enum EndpointType{
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }

}
