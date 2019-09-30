package org.mkralik.learning.lra.axon.interceptors.event;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.store.AxonLraContextInfo;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.mkralik.learning.lra.axon.store.LraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class EventHandlerInterceptor implements MessageHandlerInterceptor<EventMessage<?>> {

    @Autowired
    private NarayanaLRAClient lraClient;

    @Autowired
    private IncomingLraContextsStore incomingContextsStore;

    @Autowired
    private LraContextsStore lraContextsStore;

    @Value("${server.port}")
    int port;

    @Override
    public Object handle(UnitOfWork<? extends EventMessage<?>> unitOfWork,
                         InterceptorChain interceptorChain) throws Exception {
        GenericDomainEventMessage event = (GenericDomainEventMessage) unitOfWork.getMessage();
        log.info("EventHandlerInterceptor event: [{}].", event);
        String aggregateID = event.getAggregateIdentifier();
        URI lraContextFromMetadata = (URI) event.getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER);

        // handled event contains LRA context
        if(lraContextFromMetadata!=null){
            URI waitingContextForAggregate = incomingContextsStore.getIncomingContextForAggregate(aggregateID);
            // waiting any LRA context for joining and if yes, is it for this event
            if(waitingContextForAggregate!=null && waitingContextForAggregate.equals(lraContextFromMetadata)){
                URI recoveryParticipantUri = joinLraForTargetAggregate(lraContextFromMetadata, aggregateID);
                incomingContextsStore.deleteIncomingContextForAggregate(event.getAggregateIdentifier());
                lraContextsStore.saveContextForAggregate(aggregateID ,
                        new AxonLraContextInfo(waitingContextForAggregate, recoveryParticipantUri, ParticipantStatus.Active));
            }
        }
        return interceptorChain.proceed();
    }

    private URI joinLraForTargetAggregate(URI lraContext, String targetAggregateId) throws URISyntaxException {
        URI uri1 = lraClient.joinLRA(lraContext,
                0L,
                new URI("http://localhost:" + port + "/axonLra/compensate/" + targetAggregateId),
                new URI("http://localhost:" + port + "/axonLra/complete/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/forget/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/leave/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/after/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/status/" + targetAggregateId),
                null,null,
                new URI("http://localhost:" + port + "/axonLra/status/" + targetAggregateId),
                null,null);
        log.info("URL for join:\n {}\n{}\n{}\n",
                new URI("http://localhost:" + port + "/axonLra/compensate/" + targetAggregateId),
                new URI("http://localhost:" + port + "/axonLra/complete/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/forget/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/leave/" + targetAggregateId),
//                new URI("http://localhost:9001/axonLra/after/" + targetAggregateId),
                new URI("http://localhost:" + port + "/axonLra/status/" + targetAggregateId)
                );
        return uri1;
    }
}