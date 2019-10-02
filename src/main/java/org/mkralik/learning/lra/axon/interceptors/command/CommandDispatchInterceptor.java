package org.mkralik.learning.lra.axon.interceptors.command;

import io.narayana.lra.Current;
import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.singletonMap;

@Slf4j
@Component
public class CommandDispatchInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    @Autowired
    NarayanaLRAClient client;

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> messages) {
        return (index, command) -> {
            URI existContext = Current.peek();
            if(existContext!=null){
                log.info("The method from which the command is dispatching contains LRA context. The context is added to the metadata. Context is {}", Current.getContexts());
                command = command.andMetaData(singletonMap(LRA.LRA_HTTP_CONTEXT_HEADER, existContext));
            }
            return command;
        };
    }

}
