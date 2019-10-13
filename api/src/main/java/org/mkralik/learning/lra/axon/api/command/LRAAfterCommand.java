package org.mkralik.learning.lra.axon.api.command;

import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.eclipse.microprofile.lra.annotation.LRAStatus;

import java.net.URI;

@Value
public class LRAAfterCommand {
    @TargetAggregateIdentifier
    String id;
    URI endedLraId;
    LRAStatus status;
}
