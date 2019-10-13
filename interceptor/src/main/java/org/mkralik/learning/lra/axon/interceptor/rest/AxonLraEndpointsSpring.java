package org.mkralik.learning.lra.axon.interceptor.rest;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.modelling.command.Repository;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.interceptor.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.interceptor.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.*;

@Slf4j
@RestController
@RequestMapping("/axonLra")
public class AxonLraEndpointsSpring {

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
    ResponseEntity complete(@PathVariable("aggregateId") String aggregateId,
                            @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                            @RequestHeader(value = LRA_HTTP_PARENT_CONTEXT_HEADER, required = false) URI parentLraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector COMPLETE endpoint was called for aggregate id: {}", realAggregateId);
        try {
            Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRACompleteCommand(realAggregateId, lraId, parentLraId)));
            return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraComplete().getReturnType(), EndpointType.COMPENSATE);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ParticipantStatus.FailedToComplete.name());
        }
    }

    @PutMapping("/compensate/{aggregateId}")
    ResponseEntity compensate(@PathVariable("aggregateId") String aggregateId,
                              @RequestHeader(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                              @RequestHeader(value = LRA_HTTP_PARENT_CONTEXT_HEADER, required = false) URI parentLraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector COMPENSATE endpoint was called for aggregate id: {}", realAggregateId);
        try {
            Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRACompensateCommand(realAggregateId, lraId, parentLraId)));
            return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraCompensate().getReturnType(), EndpointType.COMPENSATE);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ParticipantStatus.FailedToCompensate.name());
        }
    }

    @GetMapping("/status/{aggregateId}")
    ResponseEntity status(@PathVariable("aggregateId") String aggregateId,
                          @RequestHeader(value = LRA_HTTP_CONTEXT_HEADER, required = false) URI lraId,
                          @RequestHeader(value = LRA_HTTP_PARENT_CONTEXT_HEADER, required = false) URI parentLraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector STATUS endpoint was called for aggregate id: {}", realAggregateId);
        try {
            Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRAStatusCommand(realAggregateId, lraId, parentLraId)));
            return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraStatus().getReturnType(), EndpointType.STATUS);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex);
        }
    }

    @DeleteMapping("/forget/{aggregateId}")
    ResponseEntity forget(@PathVariable("aggregateId") String aggregateId,
                          @RequestHeader(value = LRA_HTTP_CONTEXT_HEADER, required = false) URI lraId,
                          @RequestHeader(value = LRA_HTTP_PARENT_CONTEXT_HEADER, required = false) URI parentLraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector FORGET endpoint was called for aggregate id: {}", realAggregateId);
        try {
            Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRAForgetCommand(realAggregateId, lraId, parentLraId)));
            return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraForget().getReturnType(), EndpointType.FORGET);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex);
        }
    }

    @PutMapping(value = "/after/{aggregateId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity after(@PathVariable("aggregateId") String aggregateId,
                         @RequestHeader(value = LRA_HTTP_ENDED_CONTEXT_HEADER, required = false) URI lraEndedId,
                         @RequestBody String lraStatus) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector AFTER endpoint was called for aggregate id: {}", realAggregateId);
        Object result = aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRAAfterCommand(realAggregateId, lraEndedId, LRAStatus.valueOf(lraStatus))));
        return processResult(result, aggregateTypeInfoStore.getAggregateTypeInfo(realAggregateId).getLraAfter().getReturnType(), EndpointType.AFTER);
    }

    @PutMapping("/leave/{aggregateId}")
    void leave(@PathVariable("aggregateId") String aggregateId,
               @RequestHeader(value = LRA_HTTP_CONTEXT_HEADER, required = false) URI lraId) throws Exception {
        String realAggregateId = URLDecoder.decode(aggregateId, "UTF-8");
        log.debug("AXON LRA connector LEAVE endpoint was called for aggregate id: {}", realAggregateId);
        aggregateTypeInfoStore.getAggregate(realAggregateId).handle(GenericCommandMessage.asCommandMessage(new LRALeaveCommand(realAggregateId, lraId)));
    }

    @GetMapping("/incomingLraContext")
    ResponseEntity incomingLraContext() {
        return ResponseEntity.ok(incomingLraContextsStore.getAllIncomingContext().entrySet());
    }

    @GetMapping("/aggregateInfo")
    ResponseEntity aggregatesInfo() {
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
            throw new IllegalStateException("The return type is not valid. The application should failed after start. Invalid type is " + resultType);
        }
    }

    public enum EndpointType {
        COMPENSATE, COMPLETE, STATUS, FORGET, AFTER, LEAVE
    }
}
