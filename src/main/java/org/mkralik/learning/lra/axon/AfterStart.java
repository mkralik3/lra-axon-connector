package org.mkralik.learning.lra.axon;

import org.mkralik.learning.lra.axon.api.command.*;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfo;
import org.mkralik.learning.lra.axon.store.AggregateTypeInfoStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class AfterStart {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private AggregateTypeInfoStore aggregateTypeInfoStore;


    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        System.out.println("++++++++++hello world, I have just started up+++++++");
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

    public void fillIfMethodFits(AggregateTypeInfo aggregateInfo, Method methodForScaning){
        List<Parameter> methodParametes  = Arrays.asList(methodForScaning.getParameters());

        for (Parameter methodParameter : methodParametes) {
            if (LRACompleteCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains Complete method");
                aggregateInfo.setLraComplete(methodForScaning);
            } else if (LRACompensateCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains Compensate method");
                aggregateInfo.setLraCompensate(methodForScaning);
            } else if (LRAStatusCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains Status method");
                aggregateInfo.setLraStatus(methodForScaning);
            }else if (LRAForgetCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains Forget method");
                aggregateInfo.setLraForget(methodForScaning);
            }else if (LRALeaveCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains Leave method");
                aggregateInfo.setLraLeave(methodForScaning);
            }else if (LRAAfterCommand.class.equals(methodParameter.getType())) {
                System.out.println("Contains After method");
                aggregateInfo.setLraAfter(methodForScaning);
            }
        }
    }
}