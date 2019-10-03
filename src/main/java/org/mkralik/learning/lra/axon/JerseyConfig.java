package org.mkralik.learning.lra.axon;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.mkralik.learning.lra.axon.rest.AxonLraEndpoints;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/*")
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(WadlResource.class);
        register(AxonLraEndpoints.class);
    }
}
