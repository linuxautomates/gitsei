package io.levelops.commons.tenant_management.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

public enum TaskStatus {
    UNASSIGNED, //Not Assigned
    PENDING, //Has been picked up & id pending
    SUCCESS, //Completed Successfully
    FAILURE; //Failed

    @JsonCreator
    public static TaskStatus fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(TaskStatus.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}
