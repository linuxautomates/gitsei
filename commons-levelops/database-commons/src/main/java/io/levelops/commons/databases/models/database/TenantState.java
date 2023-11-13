package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantState {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "state")
    private State state;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    public enum State {
        CS_USER_CREATED,
        DEFAULT_USER_CREATED,
        DEMO_DASHBOARD_CREATED,
        DORA_DASHBOARD_CREATED,
        ORG_UNITS_CREATED;

        @JsonCreator
        @Nullable
        public static TenantState.State fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(TenantState.State.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}

