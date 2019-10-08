package org.mkralik.learning.lra.axon.api.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class LRAForgetCommand {
    @TargetAggregateIdentifier
    String id;
}