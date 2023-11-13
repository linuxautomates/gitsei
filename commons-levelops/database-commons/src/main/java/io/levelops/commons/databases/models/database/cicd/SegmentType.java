package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;


public enum SegmentType {
    CICD_JOB,
    CICD_STAGE,
    CICD_STEP;

    @JsonCreator
    @Nullable
    public static SegmentType fromString(final String segmentType) {
        return EnumUtils.getEnumIgnoreCase(SegmentType.class, segmentType);
    }
}
