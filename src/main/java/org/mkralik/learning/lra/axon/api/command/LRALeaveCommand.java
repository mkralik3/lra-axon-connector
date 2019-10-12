package org.mkralik.learning.lra.axon.api.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.net.URI;

@Value
public class LRALeaveCommand {
    @TargetAggregateIdentifier
    String id;
    URI lraId;
}
