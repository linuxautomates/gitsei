package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import java.util.concurrent.TimeUnit;

@Getter
public enum AggJobType {
    SCM_COMMIT_PR_MAPPING(TimeUnit.HOURS.toSeconds(1L)),
    CICD_ARTIFACT_MAPPING(null);

    private final Long taskIntervalInSecs;

    AggJobType(Long taskIntervalInSecs) {
        this.taskIntervalInSecs = taskIntervalInSecs;
    }

    @JsonCreator
    public static AggJobType fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(AggJobType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
