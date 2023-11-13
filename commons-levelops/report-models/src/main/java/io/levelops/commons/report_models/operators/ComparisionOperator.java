package io.levelops.commons.report_models.operators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum ComparisionOperator {
    EQ("=", false, false),
    NEQ("!=", true, false),
    GT(">", false, false),
    LT("<", false, false),
    GTE("<", false, false),
    LTE("<", false, false),
    IN("in", false, true),
    NIN("in", true, true);

    private final String readableFormat;
    private final boolean notCondition;
    private final boolean multiValue;

    ComparisionOperator(String readableFormat, boolean notCondition, boolean multiValue) {
        this.readableFormat = readableFormat;
        this.notCondition = notCondition;
        this.multiValue = multiValue;
    }

    @JsonCreator
    @Nullable
    public static ComparisionOperator fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(ComparisionOperator.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
