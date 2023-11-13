package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyServicesPage.PagerDutyServicesPageBuilderImpl.class)
public class PagerDutyServicesPage extends PagerDutyResponse {
    @JsonProperty("services")
    private List<Map<String, Object>> items;

    @Getter
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PagerDutyServicesPage.PagerDutyServicesPageBuilderImpl.class)
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
    static final class PagerDutyServicesPageBuilderImpl
            extends PagerDutyServicesPageBuilder<PagerDutyServicesPage, PagerDutyServicesPageBuilderImpl> {
    }

    @Override
    public PagerDutyIngestionDataType getIngestionDataType() {
        return PagerDutyIngestionDataType.SERVICE;
    }
}