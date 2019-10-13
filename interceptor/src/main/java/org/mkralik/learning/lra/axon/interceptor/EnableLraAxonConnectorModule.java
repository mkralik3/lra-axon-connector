package org.mkralik.learning.lra.axon.interceptor;

import org.mkralik.learning.lra.axon.interceptor.command.CommandDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptor.command.CommandHandlerInterceptor;
import org.mkralik.learning.lra.axon.interceptor.event.EventDispatchInterceptor;
import org.mkralik.learning.lra.axon.interceptor.event.EventHandlerInterceptor;
import org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsJaxRS;
import org.mkralik.learning.lra.axon.interceptor.rest.AxonLraEndpointsSpring;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({AxonLraConfiguration.class,
        CommandDispatchInterceptor.class,
        CommandHandlerInterceptor.class,
        EventDispatchInterceptor.class,
        EventHandlerInterceptor.class,
        AxonLraEndpointsSpring.class,
        AxonLraEndpointsJaxRS.class,
        AfterStart.class})
@Configuration
public @interface EnableLraAxonConnectorModule{
}
