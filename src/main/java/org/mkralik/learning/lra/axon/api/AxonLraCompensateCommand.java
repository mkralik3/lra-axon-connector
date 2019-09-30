package org.mkralik.learning.lra.axon.api;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class AxonLraCompensateCommand {
    @TargetAggregateIdentifier
    String id;
}