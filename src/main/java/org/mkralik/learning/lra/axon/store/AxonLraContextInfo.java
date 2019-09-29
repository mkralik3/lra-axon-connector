package org.mkralik.learning.lra.axon.store;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

import java.net.URI;

@Data
@AllArgsConstructor
public class AxonLraContextInfo {
    URI lraContext = null;
    URI recoveryLRA = null;
    ParticipantStatus status = null;
}
