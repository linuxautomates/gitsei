package io.propelo.trellis_framework.models.audit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum AuditLogSourceType {
    LEGACY_AGGS,
    TRELLIS_WORKER;

    @JsonCreator
    @Nullable
    public static AuditLogSourceType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(AuditLogSourceType.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

}