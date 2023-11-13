package io.levelops.commons.report_models.operators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

//LqlConstants
@Getter
public enum LogicalOperator {
    AND,
    OR;

    @JsonCreator
    @Nullable
    public static LogicalOperator fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(LogicalOperator.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
