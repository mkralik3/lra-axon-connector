package org.mkralik.learning.lra.axon.interceptors.event;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
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
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
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
        GenericDomainEventMessage event = (GenericDomainEventMessage) unitOfWork.getMessage();
        String aggregateID = event.getAggregateIdentifier();
        URI lraContextFromMetadata = (URI) event.getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER);

        if (lraContextFromMetadata != null) {
            URI waitingContextForAggregate = incomingContextsStore.getIncomingContextForAggregate(aggregateID);
            // handled event contains LRA context but the event can be from other parts or can be an event from the event store which was already processed
            // if any context waiting for this aggregate the event has to be fired from the Aggregate constructor and need to join to LRA.
            if (waitingContextForAggregate != null && waitingContextForAggregate.equals(lraContextFromMetadata)) {
                URI recoveryParticipantUri = joinLraForTargetAggregate(lraContextFromMetadata, aggregateID);
                incomingContextsStore.deleteIncomingContextForAggregate(event.getAggregateIdentifier());
            }
        }
        return interceptorChain.proceed();
    }

    private URI joinLraForTargetAggregate(URI lraContext, String targetAggregateId) throws UnsupportedEncodingException, URISyntaxException {
        String encodedTargetId = URLEncoder.encode(targetAggregateId, "UTF-8");

        Aggregate<?> targetAggregate = findTargetAggregate(targetAggregateId);
        AggregateTypeInfo targetAggregateTypeInfo = aggregateTypeInfoStore.getAggregateTypeInfo(targetAggregate.rootType());
        if(targetAggregateTypeInfo==null){
            throw new IllegalStateException("Aggregate type info store doesn't contains class information about aggregate");
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
     * Check whether the aggregate class contains a particular command as a parameter in some method.
     * In other words, whether aggregate class handles a particular command class.
     */
    private boolean containsCommand(Class<?> particularCommandClass, String aggregateId) {
        Aggregate<?> targetAggregate = findTargetAggregate(aggregateId);
        if (targetAggregate == null) {
            throw new IllegalStateException("Aggregate with ID " + aggregateId + " does not exist.");
        }
        Method[] methods = targetAggregate.rootType().getDeclaredMethods();
        return Arrays.stream(methods)
                .anyMatch(method -> Arrays.asList(method.getParameterTypes()).contains(particularCommandClass));
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