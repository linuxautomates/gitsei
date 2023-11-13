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

import java.util.Map;

import javax.annotation.Nullable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriageRuleHit {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "type")
    private RuleHitType type;

    @JsonProperty(value = "stage_id")
    private String stageId;

    @JsonProperty(value = "step_id")
    private String stepId;

    @JsonProperty(value = "job_run_id")
    private String jobRunId;

    @JsonProperty(value = "rule_id")
    private String ruleId;

    @JsonProperty(value = "count") //hit count
    private Integer count;

    @JsonProperty(value = "hit_content") //first 30 lines from hit
    private String hitContent;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @JsonProperty("context")
    private Map<String, Object> context;

    public enum RuleHitType {
        JENKINS;

        @JsonCreator
        @Nullable
        public static TriageRuleHit.RuleHitType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(TriageRuleHit.RuleHitType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
