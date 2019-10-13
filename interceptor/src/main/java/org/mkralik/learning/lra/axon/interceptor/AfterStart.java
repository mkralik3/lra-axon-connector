package org.mkralik.learning.lra.axon.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsSpring.EndpointType;
import org.mkralik.learning.lra.axon.interceptor.store.AggregateTypeInfo;
import org.mkralik.learning.lra.axon.interceptor.store.AggregateTypeInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsSpring.EndpointType.*;
import static org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsSpring.EndpointType.COMPENSATE;

@Component
@Slf4j
public class AfterStart {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;


    @EventListener(ApplicationReadyEvent.class)
    public void afterStartUp() {
        log.debug("The application was started. The LRA Axon connector is going to scan all aggregates to knows return types");
        scanAllAggregates();
        log.debug("Scanning was complete. The found methods have valid return types");
    }

    private void scanAllAggregates() {
        Map<String, Object> LRAParticipant = appContext.getBeansWithAnnotation(org.axonframework.spring.stereotype.Aggregate.class);
        for (Object participant : LRAParticipant.values()) {
            AggregateTypeInfo aggregateInfo = new AggregateTypeInfo();
            Class<?> participantClazz = participant.getClass();

            for (Method declaredMethod : participantClazz.getDeclaredMethods()) {
                fillIfMethodFits(aggregateInfo, declaredMethod);
            }
            aggregateTypeInfoStore.saveAggregateTypeInfo(participantClazz, aggregateInfo);
        }
    }

    private void fillIfMethodFits(AggregateTypeInfo aggregateInfo, Method methodForScanning) {
        Parameter[] methodParameters = methodForScanning.getParameters();

        for (Parameter methodParameter : methodParameters) {
            if (LRACompleteCommand.class.equals(methodParameter.getType())) {
                log.debug("The complete method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), COMPLETE);
                aggregateInfo.setLraComplete(methodForScanning);
            } else if (LRACompensateCommand.class.equals(methodParameter.getType())) {
                log.debug("The compensate method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), COMPENSATE);
                aggregateInfo.setLraCompensate(methodForScanning);
            } else if (LRAStatusCommand.class.equals(methodParameter.getType())) {
                log.debug("The status method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), STATUS);
                aggregateInfo.setLraStatus(methodForScanning);
            } else if (LRAForgetCommand.class.equals(methodParameter.getType())) {
                log.debug("The forget method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), FORGET);
                aggregateInfo.setLraForget(methodForScanning);
            } else if (LRALeaveCommand.class.equals(methodParameter.getType())) {
                log.debug("The leave method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), LEAVE);
                aggregateInfo.setLraLeave(methodForScanning);
            } else if (LRAAfterCommand.class.equals(methodParameter.getType())) {
                log.debug("The after lra method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AFTER);
                aggregateInfo.setLraAfter(methodForScanning);
            }
        }
    }

    private void validateReturnType(Class<?> returnType, EndpointType type) {
        switch (type) {
            case COMPENSATE:
            case COMPLETE:
                if (!(returnType.equals(Void.TYPE) || returnType.equals(Void.class) || returnType.equals(ParticipantStatus.class) || returnType.equals(ResponseEntity.class))) {
                    throw new IllegalStateException("The function which handles LRACompensateCommand or LRACompleteCommand" +
                            " has to return only VOID, ParticipantStatus or ResponseEntity. The invalid return type is " + returnType);
                }
                break;
            case STATUS:
                if (!returnType.equals(ParticipantStatus.class)) {
                    throw new IllegalStateException("The function which handles LRAStatusCommand has to return ParticipantStatus enum. The invalid return type is " + returnType);
                }
                break;
            case AFTER:
                if (!returnType.equals(Void.TYPE)) {
                    throw new IllegalStateException("The function which handles LRAAfterCommand must not return anything (void). The invalid return type is " + returnType);
                }
                break;
            case FORGET:
            case LEAVE:
                break;
        }
    }
}