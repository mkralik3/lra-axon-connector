package org.mkralik.learning.lra.axon.store;

import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class LraContextsStore {

    /**
     * Aggregate ID x AxonLraContextInfo
     */
    private HashMap<String, AxonLraContextInfo> lraContextForAggregateId;

    public LraContextsStore() {
        this.lraContextForAggregateId =  new HashMap<String, AxonLraContextInfo>();
    }

    public void saveContextForAggregate(String aggregateId, AxonLraContextInfo lraContext){
        lraContextForAggregateId.put(aggregateId, lraContext);
    }

    public AxonLraContextInfo deleteContextForAggregate(String aggregateId){
        return lraContextForAggregateId.remove(aggregateId);
    }

    /**
     * @return lra context for aggregate if exist, otherwise null
     */
    public AxonLraContextInfo getContextForAggregate(String aggregateId){
        return lraContextForAggregateId.getOrDefault(aggregateId, null);
    }

    public HashMap<String, AxonLraContextInfo> getAllContext(){
        return lraContextForAggregateId;
    }

}
