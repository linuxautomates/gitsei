package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import javax.validation.constraints.Pattern;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkItem implements Serializable {
    public static final TagItemMapping.TagItemType ITEM_TYPE = TagItemMapping.TagItemType.WORK_ITEM;

    public static final String UNASSIGNED_ASSIGNEE_QUERY_VALUE = "UNASSIGNED";
    private static final long serialVersionUID = 6942555429449713042L;
    @JsonProperty("status")
    public String status; //Used only during get/list for backward compatibility
    @Singular
    @JsonProperty("assignees")
    List<Assignee> assignees;
    //quiz linked through quiz.workitemid field
    //notes for workitem will go to workitemnotes table. when kb is sent, a note will be added with kb information
    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "priority")
    private Severity priority; // this will be returned through a join. will NOT be stored in workitems table today.
    @JsonProperty(value = "type")
    private ItemType type;
    @JsonProperty(value = "ticket_type")
    private TicketType ticketType;
    @JsonProperty(value = "cloud_owner")
    private String cloudOwner;
    @JsonProperty(value = "artifact_title")
    private String artifactTitle;
    @JsonProperty(value = "notify") //on change should we notify people in the ticket?
    private Boolean notify;
    @JsonProperty(value = "due_at")
    private Long dueAt;
    @JsonProperty(value = "reason")
    private String reason;
    @JsonProperty(value = "product_id")
    private String productId;
    @JsonProperty(value = "integration_id")
    private String integrationId;
    @JsonProperty(value = "artifact")
    private String artifact;
    @JsonProperty(value = "updated_at")
    private Long updatedAt;
    @JsonProperty(value = "created_at")
    private Long createdAt;
    @Pattern(regexp = "^[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}$")
    @JsonProperty("ticket_template_id")
    private String ticketTemplateId;
    @Singular
    @JsonProperty("ticket_data_values")
    private List<TicketData> ticketDataValues;
    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;
    @JsonProperty("state_id")
    private Integer stateId;
    @Singular
    @JsonProperty("attachments")
    private List<Attachment> attachments;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("reporter")
    private String reporter;

    @JsonProperty("vanity_id")
    private String vanityId;

    @JsonProperty("parent_id")
    private String parentId;

    @Singular
    @JsonProperty("child_ids")
    private List<String> childIds;

    @Singular
    @JsonProperty("cicd_mappings")
    private List<CICDMapping> cicdMappings;

    @Singular
    @JsonProperty("notifications")
    private List<Notification> notifications;

    public enum ItemStatus {
        NEW,
        OPEN,
        IN_REVIEW,
        CLOSED;

        @JsonCreator
        @Nullable
        public static ItemStatus fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(ItemStatus.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    public enum ItemType {
        MANUAL,
        AUTOMATED;

        @JsonCreator
        @Nullable
        public static ItemType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(ItemType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Getter
    public enum TicketType {
        TASK("Task"),
        REQUEST("Request"),
        WORK_ITEM("WorkItem"),
        EPIC("Epic"),
        BUG("Bug"),
        STORY("Story"),
        FAILURE_TRIAGE("FailureTriage"),
        SNIPPET("Snippet");

        private final String description;

        TicketType(String description) {
            this.description = description;
        }

        @JsonCreator
        @Nullable
        public static TicketType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(TicketType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuperBuilder(toBuilder = true)
    public static class Attachment {
        @JsonProperty("id")
        private String id;
        @JsonProperty("work_item_id")
        private String workItemId;
        @JsonProperty("upload_id")
        private String uploadId;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("comment")
        private String comment;
        @JsonProperty("uploaded_at")
        private Long uploadedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuperBuilder(toBuilder = true)
    public static class Assignee {
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("user_email")
        private String userEmail;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuperBuilder(toBuilder = true)
    public static class CICDMapping {
        @JsonProperty("cicd_job_run_id")
        private UUID cicdJobRunId;
        @JsonProperty("cicd_job_run_stage_id")
        private UUID cicdJobRunStageId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuperBuilder(toBuilder = true)
    public static class Notification {
        @JsonProperty(value = "mode")
        private NotificationMode mode;

        @JsonProperty("recipient")
        private String recipient;

        @JsonProperty("url")
        private String url;

        @JsonProperty("created_at")
        private Instant createdAt;
    }
}