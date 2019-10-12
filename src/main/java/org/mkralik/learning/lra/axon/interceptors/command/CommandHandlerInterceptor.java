package org.mkralik.learning.lra.axon.interceptors.command;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.mkralik.learning.lra.axon.api.annotation.JoinLRA;
import org.mkralik.learning.lra.axon.api.annotation.LRAContext;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
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
        if(command.getPayloadType().isAnnotationPresent(JoinLRA.class)){
            log.debug("JoinLRA annotation is found in the command: [{}].", command);
            //Arriving command wants to start LRA. The LRA context is saved in case the aggregate will be fire an event.
            URI lraContext = null;
            Optional<Field> incomingCommandLraField = Arrays.stream(command.getPayloadType().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(LRAContext.class)).findAny();

            Field targetIdentifierField = Arrays.stream(command.getPayloadType().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(TargetAggregateIdentifier.class)).findAny().get();
            String aggregateTargetIdentifier = String.valueOf(getObjectFromField(command.getPayload(), targetIdentifierField));


            if(incomingCommandLraField.isPresent()){
                // user specify context explicitly, use it and also add it to metadata
                log.debug("Context Field is present, the arrived context is overridden.");
                final URI context = lraContext = (URI) getObjectFromField(command.getPayload(), incomingCommandLraField.get());
                unitOfWork.transformMessage(event -> event
                        .andMetaData(singletonMap(LRA.LRA_HTTP_CONTEXT_HEADER, context)));
            }else if (command.getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER)!=null){
                // user doesn't specify context but the context comes from source
                log.debug("Context Field is not present, the context is used from metadata.");
                lraContext = (URI) command.getMetaData().get(LRA.LRA_HTTP_CONTEXT_HEADER);
            }else{
                throw new ConfigurationException("Command contains annotation for join LRA but context missing! " +
                        "The source method of the command doesn't belong to LRA context and the context doesn't provide explicitly by @LRAContext");
            }
            incomingLraContextsStore.saveIncomingContextForAggregate(aggregateTargetIdentifier, lraContext);
        }
        return interceptorChain.proceed();
    }

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
