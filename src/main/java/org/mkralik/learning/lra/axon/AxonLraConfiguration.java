package org.mkralik.learning.lra.axon;

import io.narayana.lra.client.NarayanaLRAClient;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.mkralik.learning.lra.axon.interceptors.event.EventDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptors.event.EventHandlerInterceptor;
import org.mkralik.learning.lra.axon.store.LraContextsStore;
import org.mkralik.learning.lra.axon.interceptors.command.CommandHandlerInterceptor;
import org.mkralik.learning.lra.axon.store.IncomingLraContextsStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class AxonLraConfiguration {

//    @Autowired
//    public void registerCommandInterceptor(CommandBus commandBus, CommandDispatchInterceptor interceptor) {
//        commandBus.registerDispatchInterceptor(interceptor);
//    }
//
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
        NarayanaLRAClient.setDefaultCoordinatorEndpoint(new URI("http://localhost:8080"));
        return new NarayanaLRAClient();
    }

    @Bean
    public LraContextsStore lraContextsStore() {
        return new LraContextsStore();
    }

    @Bean
    public IncomingLraContextsStore incomingLraContextsStore() {
        return new IncomingLraContextsStore();
    }

}
