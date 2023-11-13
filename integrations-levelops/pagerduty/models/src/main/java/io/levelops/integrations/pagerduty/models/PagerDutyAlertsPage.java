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
@JsonDeserialize(builder = PagerDutyAlertsPage.PagerDutyAlertsPageBuilderImpl.class)
public class PagerDutyAlertsPage extends PagerDutyResponse {
    @JsonProperty("alerts")
    private List<Map<String, Object>> items;

    @Value
    @Builder
    @JsonDeserialize(builder = PagerDutyAlertsPage.PagerDutyAlertsPageBuilderImpl.class)
    public static class Query implements PagerDutyDataQuery {

        @JsonProperty("integration_key")
        private IntegrationKey integrationKey;

        @JsonProperty("statuses")
        private String incidentId;

        @JsonProperty("statuses")
        private String alertId;

        @JsonProperty("statuses")
        private List<String> statuses;

        @JsonProperty("incident_key")
        private String incidentKey;

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
    static final class PagerDutyAlertsPageBuilderImpl
            extends PagerDutyAlertsPageBuilder<PagerDutyAlertsPage, PagerDutyAlertsPageBuilderImpl> {
    }

    @Override
    public PagerDutyIngestionDataType getIngestionDataType() {
        return PagerDutyIngestionDataType.ALERT;
    }
}