package org.mkralik.learning.lra.axon.interceptor;

import io.narayana.lra.client.NarayanaLRAClient;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.mkralik.learning.lra.axon.interceptor.command.CommandDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptor.command.CommandHandlerInterceptor;
import org.mkralik.learning.lra.axon.interceptor.event.EventDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptor.event.EventHandlerInterceptor;
import org.mkralik.learning.lra.axon.interceptor.store.AggregateTypeInfoStore;
import org.mkralik.learning.lra.axon.interceptor.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@Configuration
public class AxonLraConfiguration {

    @Autowired
    public void registerCommandInterceptor(CommandBus commandBus, CommandDispatchInterceptor interceptor) {
        commandBus.registerDispatchInterceptor(interceptor);
    }

    @Autowired
    public void registerCommandInterceptor(CommandBus commandBus, CommandHandlerInterceptor interceptor) {
        commandBus.registerHandlerInterceptor(interceptor);
    }

    @Autowired
    public void registerEventInterceptor(EventBus eventBus, EventDispatchInterceptor interceptor) {
        eventBus.registerDispatchInterceptor(interceptor);
    }

    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer eventProcessingConfigurer, EventHandlerInterceptor openTraceHandlerInterceptor) {
        eventProcessingConfigurer.registerDefaultHandlerInterceptor(
                (configuration, name) -> openTraceHandlerInterceptor
        );
    }

    @Bean
    public NarayanaLRAClient narayanaLRAClient() throws URISyntaxException {
        return new NarayanaLRAClient();
    }

    @Bean
    public IncomingLraContextsStore incomingLraContextsStore() {
        return new IncomingLraContextsStore();
    }

    @Bean
    public AggregateTypeInfoStore aggregateTypeInfoStore() {
        return new AggregateTypeInfoStore();
    }

}
