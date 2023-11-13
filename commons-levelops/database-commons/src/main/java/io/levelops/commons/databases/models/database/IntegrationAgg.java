package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IntegrationAgg.IntegrationAggBuilder.class)
public class IntegrationAgg {

    @JsonProperty("id")
    private String id;

    @JsonProperty("version")
    private String version;

    @JsonProperty("successful")
    private Boolean successful;

    @JsonProperty("integration_ids")
    private List<String> integrationIds;

    @JsonProperty("type")
    private AnalyticType type;

    @JsonProperty("gcs_path")
    private String gcsPath;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    public enum AnalyticType {
        //analytic names will be created in lexical app name order.
        //GITHUBxJIRA instead of JIRAxGITHUB cuz G < J
        JIRA,
        GITHUB,
        SNYK,
        PAGERDUTY,
        GITHUBxJIRA,
        BITBUCKET,
        TENABLE;

        @JsonCreator
        @Nullable
        public static AnalyticType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(AnalyticType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }

}

