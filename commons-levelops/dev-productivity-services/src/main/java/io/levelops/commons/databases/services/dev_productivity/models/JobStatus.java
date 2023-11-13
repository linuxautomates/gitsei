package io.levelops.commons.databases.services.dev_productivity.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.EnumUtils;

public enum JobStatus {
    UNASSIGNED, //Not Assigned
    PENDING, //Has been picked up & id pending
    SUCCESS, //Completed Successfully
    FAILURE; //Failed

    @JsonCreator
    public static JobStatus fromString(String value) {
        return EnumUtils.getEnumIgnoreCase(JobStatus.class, value);
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }

}
