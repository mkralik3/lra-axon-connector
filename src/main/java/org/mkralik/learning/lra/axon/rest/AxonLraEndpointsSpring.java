package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.Repository;
import org.eclipse.microprofile.lra.annotation.*;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER;

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

    private List<Repository<?>> repositories;

    @Autowired
    public void setRepositories(List<Repository<?>> repository) {
        this.repositories = repository;
    }

    @PutMapping("/complete/{aggregateId}")
    public ResponseEntity complete(@PathVariable("aggregateId") String aggregateId, @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector COMPLETE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRACompleteCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.COMPLETE);
    }

    @PutMapping("/compensate/{aggregateId}")
    public ResponseEntity compensate(@PathVariable("aggregateId") String aggregateId, @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector COMPENSATE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRACompensateCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.COMPENSATE);
    }

    @GetMapping("/status/{aggregateId}")
    public ResponseEntity status(@PathVariable("aggregateId") String aggregateId, @RequestHeader(value = LRA_HTTP_CONTEXT_HEADER, required=false) URI lraId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector STATUS endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAStatusCommand(realAggregateId, lraId));
        return processResult(result, realAggregateId, EndpointType.STATUS);
    }

    @DeleteMapping("/forget/{aggregateId}")
    public void forget(@PathVariable("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector FORGET endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAForgetCommand(realAggregateId));
        log.warn("Not implemented yet");
    }

    @PutMapping("/after/{aggregateId}")
    public void after(@PathVariable("aggregateId") String aggregateId, @RequestHeader(value = LRA_HTTP_ENDED_CONTEXT_HEADER, required=false) URI lraEndedId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector AFTER endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAAfterCommand(realAggregateId, lraEndedId));
        log.warn("Not implemented yet");
    }

    @PutMapping("/leave/{aggregateId}")
    public void leave(@PathVariable("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.info("in the AXON LRA connector LEAVE endpoint id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRALeaveCommand(realAggregateId));
        log.warn("Not implemented yet");
    }

    @GetMapping("/incomingLraContext")
    public ResponseEntity incomingLraContext() {
        return ResponseEntity.ok(incomingLraContextsStore.getAllIncomingContext().entrySet());
    }

    @GetMapping("/aggregateInfo")
    public ResponseEntity aggregatesInfo() {
        return ResponseEntity.ok(aggregateTypeInfoStore.getAllAggregatesInfo().entrySet());
    }

    private ResponseEntity processResult(Object result, String aggregateId, EndpointType type) {


//        AggregateTypeInfo aggregateTypeInfo = aggregateTypeInfoStore.getAggregateTypeInfo(aggregateResolver.findTargetAggregate(aggregateId).rootType());
//        if(aggregateTypeInfo==null){
//            throw new IllegalStateException("Aggregate type info store doesn't contains class information about aggregate");
//        }

//        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
//        if (result instanceof Response) {
//            return (Response) result;
//        }else
        if (result == null) {
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
        } else if (result instanceof ParticipantStatus) {
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                return new ResponseEntity<>(HttpStatus.ACCEPTED);
            } else {
                return ResponseEntity.ok(status.name());
            }
        } else {
            throw new IllegalStateException("not implemented yet");
        }
    }

    private enum EndpointType {
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }

}
