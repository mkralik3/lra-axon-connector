package org.mkralik.learning.lra.axon.store;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;

@Component
public class IncomingLraContextsStore {

    /**
     * Aggregate ID x LRA Context
     */
    private HashMap<String, URI> incomingLraContext;

    public IncomingLraContextsStore() {
        this.incomingLraContext =  new HashMap<String, URI>();
    }

    public void saveIncomingContextForAggregate(String aggregateId, URI lraContext){
        incomingLraContext.put(aggregateId, lraContext);
    }

    public URI deleteIncomingContextForAggregate(String aggregateId){
        return incomingLraContext.remove(aggregateId);
    }

    /**
     * @return lra context for aggregate if exist, otherwise null
     */
    public URI getIncomingContextForAggregate(String aggregateId){
        return incomingLraContext.getOrDefault(aggregateId, null);
    }

    public HashMap<String, URI> getAllIncomingContext(){
        return incomingLraContext;
    }

}
