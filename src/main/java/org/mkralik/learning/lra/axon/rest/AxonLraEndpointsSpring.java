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
import java.util.concurrent.TimeUnit;

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
    public Response complete(@PathVariable String id) {
        log.info("in the AXONLRA endpoint function complete with id: {}", id);
        lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.Completing);
        boolean result = commandGateway.sendAndWait(new AxonLraCompleteCommand(id), 2, TimeUnit.MINUTES);
        log.info("result of AxonLraCompleteCommand from aggregate is: {}", result);
        if(result){
            lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.Completed);
        }else {
            lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.FailedToComplete);
        }
        return Response.ok(lraContextsStore.getContextForAggregate(id).getStatus()).build();
    }

    @PutMapping("/compensate/{id}")
    public Response compensate(@PathVariable String id) {
        log.info("in the AXONLRA endpoint function compensate with id: {}", id);
        lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.Compensating);
        boolean result = commandGateway.sendAndWait(new AxonLraCompensateCommand(id), 2, TimeUnit.MINUTES);
        log.info("result of AxonLraCompensateCommand from aggregate is: {}", result);
        if(result){
            lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.Compensated);
        }else {
            lraContextsStore.getContextForAggregate(id).setStatus(ParticipantStatus.FailedToCompensate);
        }
        return Response.ok(lraContextsStore.getContextForAggregate(id).getStatus()).build();
    }

    @PutMapping("/leave/{id}")
    public void leave(@PathVariable String id) {
        log.info("in the AXONLRA endpoint function leave with id: {}", id);
    }

    @DeleteMapping("/forget/{id}")
    public void forget(@PathVariable String id) {
        log.info("in the AXONLRA endpoint function forget with id: {}", id);
    }

    @GetMapping("/status/{id}")
    public Response status(@PathVariable String id) {
        log.info("in the AXONLRA endpoint function status with id: {}", id);
        return Response.ok().entity(lraContextsStore.getContextForAggregate(id).getStatus()).build();
    }

//    @GetMapping("/after/{id}")
//    public Response after(@PathVariable String id) {
//        log.info("in the AXONLRA endpoint function after with id: {}", id);
//        return Response.ok().entity(status).build();
//    }

    @GetMapping("/incomingLraContext")
    public Response incomingLraContext() {
        return Response.ok().entity(incomingLraContextsStore.getAllIncomingContext().entrySet()).build();
    }

    @GetMapping("/actualLraContext")
    public Response actualLraContext() {
        return Response.ok().entity(lraContextsStore.getAllContext().entrySet()).build();
    }

}
