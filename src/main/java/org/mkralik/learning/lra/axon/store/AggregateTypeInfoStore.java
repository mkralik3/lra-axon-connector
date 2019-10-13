package org.mkralik.learning.lra.axon.store;

import org.axonframework.modelling.command.Aggregate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class AggregateTypeInfoStore {

    private Map<Class<?>, AggregateTypeInfo> aggregatesTypes;

    private Map<String, Aggregate<?>> aggregateIdAggregate;

    public AggregateTypeInfoStore() {
        this.aggregatesTypes = new HashMap<>();
        this.aggregateIdAggregate = new HashMap<>();
    }

    public void saveAggregateTypeInfo(Class<?> aggregateClass, AggregateTypeInfo aggregateInfo) {
        aggregatesTypes.put(aggregateClass, aggregateInfo);
    }

    public void saveAggregateInstance(String aggregateId, Aggregate<?> aggregate) {
        aggregateIdAggregate.putIfAbsent(aggregateId, aggregate);
    }

    /**
     * Get aggregate type info according to aggregate class
     */
    public AggregateTypeInfo getAggregateTypeInfo(String aggregateId) {
        Aggregate<?> aggregate = aggregateIdAggregate.getOrDefault(aggregateId, null);
        if (aggregate != null) {
            Class<?> aggregateClazz = aggregate.rootType();
            return aggregatesTypes.getOrDefault(aggregateClazz, null);
        } else {
            return null;
        }
    }

    public Aggregate<?> getAggregate(String aggregateId) {
        return aggregateIdAggregate.getOrDefault(aggregateId, null);
    }

    public Map<Class<?>, AggregateTypeInfo> getAllAggregatesInfo() {
        return Collections.unmodifiableMap(aggregatesTypes);
    }
}