package org.mkralik.learning.lra.axon.interceptors.command;

import io.narayana.lra.client.NarayanaLRAClient;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@Component
public class CommandDispatchInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    @Autowired
    NarayanaLRAClient client;

    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(List<? extends CommandMessage<?>> messages) {
        return (index, command) -> {

            log.info("CommandDispatchInterceptor command: [{}].", command);
            return command;
        };
    }

}
