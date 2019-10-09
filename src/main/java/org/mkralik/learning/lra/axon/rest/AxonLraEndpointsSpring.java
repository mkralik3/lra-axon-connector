package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.eclipse.microprofile.lra.annotation.*;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfo;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/axonLra")
public class AxonLraEndpointsSpring {

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;

    @PutMapping("/complete/{aggregateId}")
    public ResponseEntity complete(@PathVariable("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector COMPLETE endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRACompleteCommand(realId)), EndpointType.COMPLETE);
    }

    @PutMapping("/compensate/{aggregateId}")
    public ResponseEntity compensate(@PathVariable("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector COMPENSATE endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRACompensateCommand(realId)), EndpointType.COMPENSATE);
    }

    @GetMapping("/status/{aggregateId}")
    public ResponseEntity status(@PathVariable("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector STATUS endpoint id: {}", realId);
        return processResult(commandGateway.sendAndWait(new LRAStatusCommand(realId)), EndpointType.STATUS);
    }

    @DeleteMapping("/forget/{aggregateId}")
    public void forget(@PathVariable("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector FORGET endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRAForgetCommand(realId));
        log.warn("Not implemented");
    }

    @PutMapping("/after/{aggregateId}")
    public void after(@PathVariable("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector AFTER endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRAAfterCommand(realId));
        log.warn("Not implemented");
    }

    @PutMapping("/leave/{aggregateId}")
    public void leave(@PathVariable("aggregateId")  String aggregateId) throws UnsupportedEncodingException {
        String realId = URLDecoder.decode( aggregateId, "UTF-8" );
        log.info("in the AXON LRA connector LEAVE endpoint id: {}", realId);
        commandGateway.sendAndWait(new LRALeaveCommand(realId));
        log.warn("Not implemented");
    }

    @GetMapping("/incomingLraContext")
    public Set<Map.Entry<String, URI>> incomingLraContext() {
        return incomingLraContextsStore.getAllIncomingContext().entrySet();
    }

    @GetMapping("/aggregateInfo")
    public Set<Map.Entry<Class<?>, AggregateTypeInfo>> aggregatesInfo() {
        return aggregateTypeInfoStore.getAllAggregatesInfo().entrySet();
    }

    private ResponseEntity processResult(Object result, EndpointType type) {
//        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
//        if (result instanceof Response) {
//            return (Response) result;
//        }else
          if (result==null) {
            // method returns `null` or nothing (void)
            switch (type) {
                case COMPLETE:
                    return ResponseEntity.ok(ParticipantStatus.Completed.name());
                case COMPENSATE:
                    return ResponseEntity.ok(ParticipantStatus.Compensated.name());
                case STATUS:
                    throw new IllegalStateException("Status method cannot return null or void");
                default:
                    //afterLra, forget,leave
                    return ResponseEntity.ok(null);
            }
        }else if(result instanceof ParticipantStatus){
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                return new ResponseEntity<>(HttpStatus.ACCEPTED);
            } else {
                return ResponseEntity.ok(status.name());
            }
        } else{
            throw new IllegalStateException("not implemented yet");
        }
    }

    private enum EndpointType{
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }

}
