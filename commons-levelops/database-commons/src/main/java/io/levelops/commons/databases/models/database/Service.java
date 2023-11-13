package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.EnumUtils;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nullable;

@Data
@Builder(toBuilder = true)
public class Service {
    private UUID id;
    private Type type;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public static enum Type {
        PAGERDUTY;

        @JsonCreator
        @Nullable
        public static Type fromString(@NonNull final String value) {
            return EnumUtils.getEnumIgnoreCase(Type.class, value);
        }
    }
}