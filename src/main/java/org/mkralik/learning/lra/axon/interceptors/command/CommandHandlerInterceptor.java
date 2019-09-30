package org.mkralik.learning.lra.axon.interceptors.command;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.api.JoinLRA;
import org.mkralik.learning.lra.axon.api.LRAContext;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Collections.singletonMap;

@Slf4j
@Component
public class CommandHandlerInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    @Autowired
    private IncomingLraContextsStore incomingLraContextsStore;

    @Override
    public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
        CommandMessage<?> command = unitOfWork.getMessage();
        log.info("CommandHandlerInterceptor command: [{}].", command);
        if(command.getPayloadType().isAnnotationPresent(JoinLRA.class)){
            //Arriving command wants to start LRA. The LRA context is saved in case the aggregate will be fire an event.
            URI lraContext = null;

            Optional<Field> incomingLraField = Arrays.stream(command.getPayloadType().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(LRAContext.class)).findAny();
            Optional<Field> targetIdentifierField = Arrays.stream(command.getPayloadType().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(TargetAggregateIdentifier.class)).findAny();

            if (!incomingLraField.isPresent()){
                log.error("Command wants to start LRA but the LRAContext annotation with context missing");
            }else{
                lraContext = (URI) getObjectFromField(command.getPayload(), incomingLraField.get());
            }
            String targetIdentifier = String.valueOf(getObjectFromField(command.getPayload(), targetIdentifierField.get()));

            incomingLraContextsStore.saveIncomingContextForAggregate(targetIdentifier, lraContext);
            command.andMetaData(singletonMap(LRA.LRA_HTTP_CONTEXT_HEADER, lraContext));
        }

        return interceptorChain.proceed();
    }

    // the type of object is not know ?
    private Object getObjectFromField(Object object, Field field) throws IllegalAccessException {
        Object result = null;
        if(!field.isAccessible()){
            field.setAccessible(true);
            result = field.get(object);
            field.setAccessible(false);
        }else{
            result = field.get(object);
        }
        return result;
    }
}
