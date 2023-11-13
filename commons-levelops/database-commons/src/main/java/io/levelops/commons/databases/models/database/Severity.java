package io.levelops.commons.databases.models.database;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.apache.commons.lang3.EnumUtils;

import lombok.Getter;

@Getter
public enum Severity{
    HIGH(3),
    MEDIUM(2),
    LOW(1),
    UNKNOWN(0);

    final private Integer value;

    Severity(final Integer value){
        this.value = value;
    }

    @JsonCreator
    @Nullable
    public static Severity fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(Severity.class, value);
    }

    @Nullable
    public static Severity fromIntValue(@Nullable Integer value) {
        for(Severity a:Severity.values()){
            if(a.value.equals(value)){
                return a;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString();
    }
}