package io.levelops.commons.enviornment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
public enum PropeloEnvironmentType {
    DEV ("dev"),
    DEV2 ("dev2"),
    STAGING ("staging"),
    PROD ("prod"),
    ASIA1 ("asia1"),
    EU1 ("eu1"),
    HARNESS_PRE_QA ("preqa-setup"),
    HARNESS_QA ("qa-setup"),
    HARNESS_PROD ("prod-setup");

    private String envName;
    PropeloEnvironmentType(String envName) {
        this.envName = envName;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.getEnvName();
    }

    @JsonCreator
    @Nullable
    public static PropeloEnvironmentType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(PropeloEnvironmentType.class, value);
    }

    public boolean isProd() {
        return this == PropeloEnvironmentType.ASIA1 || this == PropeloEnvironmentType.PROD || this == PropeloEnvironmentType.EU1
                || this == PropeloEnvironmentType.HARNESS_PROD;
    }

}