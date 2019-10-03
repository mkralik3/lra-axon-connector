package org.mkralik.learning.lra.axon.interceptors.event;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

@Slf4j
@Component
public class EventHandlerInterceptor implements MessageHandlerInterceptor<EventMessage<?>> {

    @Autowired
    private NarayanaLRAClient lraClient;

    @Autowired
    private IncomingLraContextsStore incomingContextsStore;

    @Value("${server.port}")
    int port;

    @Override
    public Object handle(UnitOfWork<? extends EventMessage<?>> unitOfWork,
                         InterceptorChain interceptorChain) throws Exception {
        GenericDomainEventMessage event = (GenericDomainEventMessage) unitOfWork.getMessage();
        String aggregateID = event.getAggregateIdentifier();
        URI lraContextFromMetadata = (URI) event.getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER);

        if(lraContextFromMetadata!=null){
            URI waitingContextForAggregate = incomingContextsStore.getIncomingContextForAggregate(aggregateID);
            // handled event contains LRA context but the event can be from other parts or can be an event from the event store which was already processed
            // if any context waiting for this aggregate the event has to be fired from the Aggregate constructor and need to join to LRA.
            if(waitingContextForAggregate!=null && waitingContextForAggregate.equals(lraContextFromMetadata)){
                URI recoveryParticipantUri = joinLraForTargetAggregate(lraContextFromMetadata, aggregateID);
                incomingContextsStore.deleteIncomingContextForAggregate(event.getAggregateIdentifier());
            }
        }
        return interceptorChain.proceed();
    }

    private URI joinLraForTargetAggregate(URI lraContext, String targetAggregateId) throws URISyntaxException, UnsupportedEncodingException {
        String encodedTargetId = URLEncoder.encode( targetAggregateId, "UTF-8" );
        URI recoveryUri = lraClient.joinLRA(lraContext,
                0L,
                new URI("http://localhost:" + port + "/axonLra/compensate/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/complete/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/forget/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/leave/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/after/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/status/" + encodedTargetId),
                null);
        log.info("URL for join:\n {}\n{}\n{}\n{}\n{}\n{}\n",
                new URI("http://localhost:" + port + "/axonLra/compensate/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/complete/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/forget/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/leave/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/after/" + encodedTargetId),
                new URI("http://localhost:" + port + "/axonLra/status/" + encodedTargetId)
                );
        return recoveryUri;
    }
}