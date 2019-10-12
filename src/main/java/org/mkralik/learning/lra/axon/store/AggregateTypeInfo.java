package org.mkralik.learning.lra.axon.store;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

@Data
@NoArgsConstructor
public class AggregateTypeInfo {
    @JsonIgnore
    Method lraCompensate;
    @JsonIgnore
    Method lraComplete;
    @JsonIgnore
    Method lraStatus;
    @JsonIgnore
    Method lraForget;
    @JsonIgnore
    Method lraLeave;
    @JsonIgnore
    Method lraAfter;

    @JsonGetter("lraCompensate")
    public String getLraCompensateForJson() {
        return (lraCompensate != null) ? lraCompensate.toString() : null;
    }

    @JsonGetter("lraComplete")
    public String getLraCompleteForJson() {
        return (lraComplete != null) ? lraComplete.toString() : null;
    }

    @JsonGetter("lraStatus")
    public String getLraStatusForJson() {
        return (lraStatus != null) ? lraStatus.toString() : null;
    }

    @JsonGetter("lraForget")
    public String getLraForgetForJson() {
        return (lraForget != null) ? lraForget.toString() : null;
    }

    @JsonGetter("lraLeave")
    public String getLraLeaveForJson() {
        return (lraLeave != null) ? lraLeave.toString() : null;
    }

    @JsonGetter("lraAfter")
    public String getLraAfterForJson() {
        return (lraAfter != null) ? lraAfter.toString() : null;
    }

    @Override
    public String toString() {
        return "AggregateTypeInfo{" +
                "lraCompensate=" + ((lraCompensate != null) ? lraCompensate.getName() : null) +
                ", lraComplete=" + ((lraComplete != null) ? lraComplete.getName() : null) +
                ", lraStatus=" + ((lraStatus != null) ? lraStatus.getName() : null) +
                ", lraForget=" + ((lraForget != null) ? lraForget.getName() : null) +
                ", lraLeave=" + ((lraLeave != null) ? lraLeave.getName() : null) +
                ", lraAfter=" + ((lraAfter != null) ? lraAfter.getName() : null) +
                '}';
    }
}