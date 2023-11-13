package io.levelops.commons.databases.models.database.automation_rules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutomationRuleHit {
    @JsonProperty(value = "id")
    private UUID id;

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("object_type")
    ObjectType objectType;

    @JsonProperty(value = "rule_id")
    private UUID ruleId;

    @JsonProperty("rule_name")
    private String ruleName; //Used only during get

    @JsonProperty(value = "count") //hit count
    private Integer count;

    @JsonProperty(value = "hit_content") //first 30 lines from hit
    private String hitContent;

    @JsonProperty("context")
    private Map<String, Object> context;

    @JsonProperty("created_at")
    Instant createdAt;

    @JsonProperty("updated_at")
    Instant updatedAt;
}
