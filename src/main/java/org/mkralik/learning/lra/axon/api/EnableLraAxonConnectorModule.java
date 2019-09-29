package org.mkralik.learning.lra.axon.api;

import org.mkralik.learning.lra.axon.AxonLraConfiguration;
import org.mkralik.learning.lra.axon.interceptors.event.EventDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptors.event.EventHandlerInterceptor;
import org.mkralik.learning.lra.axon.CustomizationPort;
import org.mkralik.learning.lra.axon.interceptors.command.CommandDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptors.command.CommandHandlerInterceptor;
import org.mkralik.learning.lra.axon.rest.AxonLraEndpointsSpring;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({AxonLraConfiguration.class, CommandDispatchInterceptor.class, EventDispatchInterceptor.class, CommandHandlerInterceptor.class, EventHandlerInterceptor.class, CustomizationPort.class, AxonLraEndpointsSpring.class})
@Configuration
public @interface EnableLraAxonConnectorModule{
}
