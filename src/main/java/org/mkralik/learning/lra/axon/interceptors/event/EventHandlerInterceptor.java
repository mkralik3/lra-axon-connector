package org.mkralik.learning.lra.axon.interceptors.event;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventsourcing.IncompatibleAggregateException;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Aggregate;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.axonframework.modelling.command.Repository;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfo;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@Component
public class EventHandlerInterceptor implements MessageHandlerInterceptor<EventMessage<?>> {

    @Autowired
    private NarayanaLRAClient lraClient;

    @Autowired
    private IncomingLraContextsStore incomingContextsStore;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;

    @Value("${server.port}")
    private int port;

    private List<Repository<?>> repositories;

    @Autowired
    public void setRepositories(List<Repository<?>> repository) {
        this.repositories = repository;
    }

    @Override
    public Object handle(UnitOfWork<? extends EventMessage<?>> unitOfWork,
                         InterceptorChain interceptorChain) throws Exception {
        URI lraContextFromMetadata = (URI) unitOfWork.getMessage().getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER);

        if (lraContextFromMetadata != null) {
            String aggregateID = ((DomainEventMessage) unitOfWork.getMessage()).getAggregateIdentifier();
            Aggregate<?> targetAggregate = findTargetAggregate(aggregateID);

            //Save aggregate instance for lra coordinator (cannot be saved after joining because during
            // the recreation of application state from the event store, the join is not performed again
            aggregateTypeInfoStore.saveAggregateInstance(aggregateID, targetAggregate);

            //If the event is a result of join LRA command, the LRA context for this aggregate has to be in the incoming context store.
            // When the application state is being recreated from event store (e.g. after the crash),
            // there is no waiting context in the incoming context store because the aggregate was already joined to lra and it is not going to call joinLRA again.
            URI waitingContextForAggregate = incomingContextsStore.getIncomingContextForAggregate(aggregateID);
            // null means that no LRA context is waiting for this aggregate (targetID)
            if (waitingContextForAggregate != null && waitingContextForAggregate.equals(lraContextFromMetadata)) {
                URI recoveryParticipantUri = joinLraForTargetAggregate(lraContextFromMetadata, aggregateID);
                // the aggregate is joined to the LRA context, the saved LRA context in the incoming context store is not needed anymore.
                incomingContextsStore.deleteIncomingContextForAggregate(aggregateID);
            }
        }
        return interceptorChain.proceed();
    }

    private URI joinLraForTargetAggregate(URI lraContext, String targetAggregateId) throws UnsupportedEncodingException, URISyntaxException {
        String encodedTargetId = URLEncoder.encode(targetAggregateId, "UTF-8");

        AggregateTypeInfo targetAggregateTypeInfo = aggregateTypeInfoStore.getAggregateTypeInfo(targetAggregateId);
        if(targetAggregateTypeInfo==null){
            throw new IllegalStateException("Aggregate type info store doesn't contains information about aggregate. The info about aggregate type is saved after start the application.");
        }

        URI compensate = targetAggregateTypeInfo.getLraCompensate()!=null ?
                new URI("http://localhost:" + port + "/axonLra/compensate/" + encodedTargetId) : null;
        URI complete = targetAggregateTypeInfo.getLraComplete()!=null ?
                new URI("http://localhost:" + port + "/axonLra/complete/" + encodedTargetId) : null;
        URI forget = targetAggregateTypeInfo.getLraForget()!=null ?
                new URI("http://localhost:" + port + "/axonLra/forget/" + encodedTargetId) : null;
        URI leave = targetAggregateTypeInfo.getLraLeave()!=null ?
                new URI("http://localhost:" + port + "/axonLra/leave/" + encodedTargetId) : null;
        URI after = targetAggregateTypeInfo.getLraAfter()!=null ?
                new URI("http://localhost:" + port + "/axonLra/after/" + encodedTargetId) : null;
        URI status = targetAggregateTypeInfo.getLraStatus()!=null ?
                new URI("http://localhost:" + port + "/axonLra/status/" + encodedTargetId) : null;

        URI recoveryUri = lraClient.joinLRA(lraContext,
                0L, compensate, complete, forget, leave, after, status, null);
        log.info("URLs for join:\n" +
                "Compensate: {}\n" +
                "Complete: {}\n" +
                "Forget: {}\n" +
                "Leave: {}\n" +
                "After: {}\n" +
                "Status: {}\n", compensate, complete, forget, leave, after, status);
        return recoveryUri;
    }

    /**
     * Method tries to find an aggregate with the target aggregate id in the all registered repositories (in JVM)
     *
     * @return found aggregate or null if no aggregate with particular id exist
     */
    private Aggregate<?> findTargetAggregate(String targetAggregateId) {
        Aggregate<?> targetAggregate = null;
        for (Repository<?> repository : repositories) {
            try {
                targetAggregate = repository.load(targetAggregateId);
                break; //found, no need to continue
            } catch (AggregateNotFoundException | IncompatibleAggregateException ex) {
                log.info("Aggregate was not found in this repo, continue");
            }
        }
        return targetAggregate;
    }
}