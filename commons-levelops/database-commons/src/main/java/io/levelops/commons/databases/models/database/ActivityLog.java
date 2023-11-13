package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityLog {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "email") //email of the user that did the activity
    private String email;

    @JsonProperty(value = "body")
    private String body;

    @JsonProperty(value = "target_item")
    private String targetItem;

    @JsonProperty(value = "target_item_type")
    private TargetItemType targetItemType;

    @JsonProperty(value = "action")
    private Action action;

    @JsonProperty(value = "details")
    private Map<String, Object> details;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    public enum TargetItemType {
        KB,
        TAG,
        USER,
        PRODUCT,
        WORKFLOW,
        SECTION,
        WORK_ITEM,
        SSO_CONFIG,
        USER_LOGIN,
        ASSESSMENT,
        INTEGRATION,
        API_ACCESS_KEY,
        ASSESSMENT_TEMPLATE,
        TICKET_TEMPLATE,
        TICKET,
        STATE,
        PLAYBOOK,
        CONFIG_TABLE,
        DASHBOARD;


        @JsonCreator
        @Nullable
        public static TargetItemType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(TargetItemType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    public enum Action {
        FAIL,
        SENT,
        EDITED,
        DELETED,
        SUCCESS,
        CREATED,
        ANSWERED,
        SUBMITTED,
        PASSWORD_RESET_STARTED, //email sent
        PASSWORD_RESET_FINISHED; //successful reset

        @JsonCreator
        @Nullable
        public static Action fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Action.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}