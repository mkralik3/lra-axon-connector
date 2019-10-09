package org.mkralik.learning.lra.axon.store;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class AggregateTypeInfoStore {

    public Map<Class<?>, AggregateTypeInfo> aggregatesTypes;

    public AggregateTypeInfoStore() {
        this.aggregatesTypes =  new HashMap<>();
    }

    public void saveAggregateTypeInfo(Class<?> aggregateClass, AggregateTypeInfo aggregateInfo){
        aggregatesTypes.put(aggregateClass, aggregateInfo);
    }

    /**
     * Get aggregate type info according to aggregate class
     */
    public AggregateTypeInfo getAggregateTypeInfo(Class<?> aggregateClass){
        return aggregatesTypes.getOrDefault(aggregateClass, null);
    }

    public Map<Class<?>, AggregateTypeInfo> getAllAggregatesInfo(){
        return Collections.unmodifiableMap(aggregatesTypes);
    }
}