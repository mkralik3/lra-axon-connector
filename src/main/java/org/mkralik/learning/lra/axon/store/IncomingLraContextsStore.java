package org.mkralik.learning.lra.axon.store;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class IncomingLraContextsStore {

    /**
     * Aggregate ID x LRA Context
     */
    private HashMap<String, URI> incomingLraContext;

    public IncomingLraContextsStore() {
        this.incomingLraContext = new HashMap<>();
    }

    public void saveIncomingContextForAggregate(String aggregateId, URI lraContext) {
        incomingLraContext.put(aggregateId, lraContext);
    }

    public URI deleteIncomingContextForAggregate(String aggregateId) {
        return incomingLraContext.remove(aggregateId);
    }

    /**
     * Get waiting incoming context
     *
     * @param aggregateId particular target aggregate identifier
     * @return lra context for aggregate if exist, otherwise null
     */
    public URI getIncomingContextForAggregate(String aggregateId) {
        return incomingLraContext.getOrDefault(aggregateId, null);
    }

    public Map<String, URI> getAllIncomingContext() {
        return Collections.unmodifiableMap(incomingLraContext);
    }

}
