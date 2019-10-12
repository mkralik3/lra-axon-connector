package org.mkralik.learning.lra.axon.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.Repository;
import org.eclipse.microprofile.lra.annotation.*;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity complete(@PathVariable("aggregateId") String aggregateId, @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector COMPLETE endpoint was called for aggregate id: {}", realAggregateId);
        Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRACompleteCommand(realAggregateId, lraId)));
        return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraComplete().getReturnType(), EndpointType.COMPENSATE);
    }

    @PutMapping("/compensate/{aggregateId}")
    public ResponseEntity compensate(@PathVariable("aggregateId") String aggregateId, @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector COMPENSATE endpoint was called for aggregate id: {}", realAggregateId);
        Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRACompensateCommand(realAggregateId, lraId)));
        return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraCompensate().getReturnType(), EndpointType.COMPENSATE);
    }

    @GetMapping("/status/{aggregateId}")
    public ResponseEntity status(@PathVariable("aggregateId") String aggregateId, @RequestHeader(value = LRA_HTTP_CONTEXT_HEADER, required=false) URI lraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector STATUS endpoint was called for aggregate id: {}", realAggregateId);
        Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRAStatusCommand(realAggregateId, lraId)));
        return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraStatus().getReturnType(), EndpointType.STATUS);
    }

    @DeleteMapping("/forget/{aggregateId}")
    public void forget(@PathVariable("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector FORGET endpoint was called for aggregate id: {}", realAggregateId);
        Object result = commandGateway.sendAndWait(new LRAForgetCommand(realAggregateId));
        log.warn("Not implemented yet");
    }

    @PutMapping("/after/{aggregateId}")
    public ResponseEntity after(@PathVariable("aggregateId") String aggregateId, @RequestHeader(value = LRA_HTTP_ENDED_CONTEXT_HEADER, required=false) URI lraEndedId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector AFTER endpoint was called for aggregate id: {}", realAggregateId);
        Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRAAfterCommand(realAggregateId, lraEndedId)));
        return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraAfter().getReturnType(), EndpointType.AFTER);
    }

    @PutMapping("/leave/{aggregateId}")
    public void leave(@PathVariable("aggregateId") String aggregateId) throws UnsupportedEncodingException {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector LEAVE endpoint was called for aggregate id: {}", realAggregateId);
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

    private ResponseEntity processResult(Object result, Class<?> resultType, EndpointType type) {
        log.debug("The result of command is going to be processed");
        if (resultType.equals(Void.TYPE) || resultType.equals(Void.class)) {
            log.debug("PROCESS RESULT: The expected result type is VOID");
            return ResponseEntity.ok().body(type.equals(EndpointType.COMPLETE) ? ParticipantStatus.Completed.name() : ParticipantStatus.Compensated.name());
        } else if (result == null) {
            log.debug("PROCESS RESULT: The expected result type is not VOID but the result is null");
            return ResponseEntity.notFound().build();
        } else if (result instanceof ParticipantStatus) {
            ParticipantStatus status = (ParticipantStatus) result;
            if (status == ParticipantStatus.Compensating || status == ParticipantStatus.Completing) {
                log.debug("PROCESS RESULT: The result type is ParticipantStatus but still in Compensating/Completing state");
                return ResponseEntity.accepted().build();
            } else {
                log.debug("PROCESS RESULT: The result type is ParticipantStatus");
                return ResponseEntity.ok().body(status.name());
            }
        } else if (result instanceof ResponseEntity) {
            log.debug("PROCESS RESULT: The result type is ResponseEntity with status {}", ((ResponseEntity) result).getStatusCode());
            return (ResponseEntity) result;
        } else {
            //builder.entity(processThrowable((Throwable) result, type));
            throw new IllegalStateException("The return type is not valid.");
        }
    }

    public enum EndpointType {
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }
}
