package org.mkralik.learning.lra.axon.interceptors.event;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.messaging.MetaData;
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
    private IncomingLraContextsStore incomingLraContextsStore;

    @Override
    public BiFunction<Integer, EventMessage<?>, EventMessage<?>> handle(List<? extends EventMessage<?>> messages) {
        return (index, event) -> {
            log.info("EventDispatchInterceptor event: [{}].", event);
            String aggregateIdentifier = ((GenericDomainEventMessage)event).getAggregateIdentifier();
            URI lraContext = incomingLraContextsStore.getIncomingContextForAggregate(aggregateIdentifier);
            if(lraContext!=null){
                //For this aggregate is waiting LRA context, add it to the event metadata
                event = event.andMetaData(singletonMap(LRA.LRA_HTTP_CONTEXT_HEADER, lraContext));
            }
            return event;
        };
    }
}