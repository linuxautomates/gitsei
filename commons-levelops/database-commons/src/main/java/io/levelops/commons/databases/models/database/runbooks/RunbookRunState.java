package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

public enum RunbookRunState {
    RUNNING(false),
    SUCCESS(true),
    FAILURE(true);

    @Getter
    private final boolean isFinal;

    RunbookRunState(boolean isFinal) {
        this.isFinal = isFinal;
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    @JsonCreator
    @Nullable
    public static RunbookRunState fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(RunbookRunState.class, value);
    }
}
