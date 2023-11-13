package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyUsersPage.PagerDutyUsersPageBuilderImpl.class)
public class PagerDutyUsersPage extends PagerDutyResponse {
    @JsonProperty("users")
    private List<Map<String, Object>> items;

    @Value
    @Builder
    @JsonDeserialize(builder = PagerDutyUsersPage.PagerDutyUsersPageBuilderImpl.class)
    public static class Query implements PagerDutyDataQuery {

        @JsonProperty("integration_key")
        private IntegrationKey integrationKey;

        @JsonProperty("since")
        private String since;

        @JsonProperty("until")
        private String until;

        @JsonProperty("offset")
        private int offset;

        @JsonProperty("limit")
        protected int limit;
    }

    @JsonPOJOBuilder(withPrefix = "")
    static final class PagerDutyUsersPageBuilderImpl
            extends PagerDutyUsersPageBuilder<PagerDutyUsersPage, PagerDutyUsersPageBuilderImpl> {
    }

    @Override
    public PagerDutyIngestionDataType getIngestionDataType() {
        return PagerDutyIngestionDataType.USER;
    }
}