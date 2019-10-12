package org.mkralik.learning.lra.axon;

import lombok.extern.slf4j.Slf4j;
import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.rest.AxonLraEndpointsSpring;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfo;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

@Component
@Slf4j
public class AfterStart {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;


    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        log.debug("The application was started. The LRA Axon connector is going to scan all aggregates to knows return types");
        scanAllAggregates();
        log.debug("Scanning was complete. The found methods have valid return types");
    }

    private void scanAllAggregates(){
        Map<String, Object> LRAParticipant = appContext.getBeansWithAnnotation(org.axonframework.spring.stereotype.Aggregate.class);
        for(Object participant : LRAParticipant.values()) {
            AggregateTypeInfo aggregateInfo = new AggregateTypeInfo();
            Class<?> participantClazz = participant.getClass();

            for (Method declaredMethod : participantClazz.getDeclaredMethods()) {
                fillIfMethodFits(aggregateInfo, declaredMethod);
            }
            aggregateTypeInfoStore.saveAggregateTypeInfo(participantClazz, aggregateInfo);
        }
    }

    private void fillIfMethodFits(AggregateTypeInfo aggregateInfo, Method methodForScanning){
        Parameter[] methodParameters  = methodForScanning.getParameters();

        for (Parameter methodParameter : methodParameters) {
            if (LRACompleteCommand.class.equals(methodParameter.getType())) {
                log.debug("The complete method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.COMPLETE);
                aggregateInfo.setLraComplete(methodForScanning);
            } else if (LRACompensateCommand.class.equals(methodParameter.getType())) {
                log.debug("The compensate method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.COMPENSATE);
                aggregateInfo.setLraCompensate(methodForScanning);
            } else if (LRAStatusCommand.class.equals(methodParameter.getType())) {
                log.debug("The status method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.STATUS);
                aggregateInfo.setLraStatus(methodForScanning);
            }else if (LRAForgetCommand.class.equals(methodParameter.getType())) {
                log.debug("The forget method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.FORGET);
                aggregateInfo.setLraForget(methodForScanning);
            }else if (LRALeaveCommand.class.equals(methodParameter.getType())) {
                log.debug("The leave method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.LEAVE);
                aggregateInfo.setLraLeave(methodForScanning);
            }else if (LRAAfterCommand.class.equals(methodParameter.getType())) {
                log.debug("The after lra method is found in the aggregate class {}", methodForScanning.getDeclaringClass());
                validateReturnType(methodForScanning.getReturnType(), AxonLraEndpointsSpring.EndpointType.AFTER);
                aggregateInfo.setLraAfter(methodForScanning);
            }
        }
    }

    private void validateReturnType(Class<?> returnType, AxonLraEndpointsSpring.EndpointType type){

    }
}