package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbWebhookData {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private Integer integrationId;

    @JsonProperty("webhook_id")
    private String webhookId;

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("webhook_event")
    private GithubWebhookEvent webhookEvent;

    @JsonProperty(value = "updated_at")
    private Instant updatedAt;

    @JsonProperty(value = "created_at")
    private Instant createdAt;

    public enum Status {
        ENQUEUED,
        JOB_FAIL,
        WRITE_FAIL,
        ENRICH_FAIL,
        SUCCESS;

        @JsonCreator
        @Nullable
        public static DbWebhookData.Status fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(DbWebhookData.Status.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
