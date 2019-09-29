package org.mkralik.learning.lra.axon.interceptors.event;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.IncompatibleAggregateException;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.modelling.command.Aggregate;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.axonframework.modelling.command.Repository;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.singletonMap;

@Slf4j
@Component
public class EventDispatchInterceptor implements MessageDispatchInterceptor<EventMessage<?>> {

    @Autowired
    NarayanaLRAClient client;

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    private List<Repository<?>> repositories;

    @Autowired
    public void setRepositories(List<Repository<?>> repository){
        this.repositories = repository;
    }

    @Override
    public BiFunction<Integer, EventMessage<?>, EventMessage<?>> handle(List<? extends EventMessage<?>> messages) {
        return (index, event) -> {
            log.info("EventDispatchInterceptor event: [{}].", event);
            String aggregateIdentifier = ((GenericDomainEventMessage)event).getAggregateIdentifier();
            URI lraContext = incomingLraContextsStore.getIncomingContextForAggregate(aggregateIdentifier);
            if(lraContext!=null){
                //For this aggregate is saved LRA context, add to metadata
                event = event.andMetaData(singletonMap(LRA.LRA_HTTP_CONTEXT_HEADER, lraContext));
            }
            return event;
        };
    }

    /**
     * Method tries to find an aggregate with the target aggregate id in the all registered repositories (in JVM)
     * @param targetAggregateId - id of an aggregate
     * @return found aggregate or null if no aggregate with particular id exist
     */
    private Aggregate<?> findTargetAggregate(String targetAggregateId){
        Aggregate<?> targetAggregate = null;
        for (Repository<?> repository : repositories) {
            try{
                targetAggregate = repository.load(targetAggregateId);
                break; //found, no need to continue
            }catch(AggregateNotFoundException | IncompatibleAggregateException ex){
                log.info("Aggregate was not found in this repo, continue");
            }
        }
        return targetAggregate;
    }
}