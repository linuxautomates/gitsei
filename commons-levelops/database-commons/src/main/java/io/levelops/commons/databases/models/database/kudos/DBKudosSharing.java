package io.levelops.commons.databases.models.database.kudos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBKudosSharing.DBKudosSharingBuilderImpl.class)
public class DBKudosSharing {
    UUID id;
    UUID kudosId;
    KudosSharingType type;
    String target;
    Instant createdAt;

    @JsonPOJOBuilder(withPrefix = "")
    static final class DBKudosSharingBuilderImpl extends DBKudosSharing.DBKudosSharingBuilder {

    }

    public enum KudosSharingType {
        SLACK,
        EMAIL;
        
        @JsonCreator
        @Nullable
        public static KudosSharingType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(KudosSharingType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
