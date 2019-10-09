package org.mkralik.learning.lra.axon.api.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.net.URI;

@Value
public class LRAAfterCommand {
    @TargetAggregateIdentifier
    String id;
    URI lraEndedId;
}
