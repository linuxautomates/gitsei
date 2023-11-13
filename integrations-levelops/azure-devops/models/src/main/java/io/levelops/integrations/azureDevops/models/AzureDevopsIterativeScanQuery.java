package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.integrations.azureDevops.utils.JobCategory;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsIterativeScanQuery.AzureDevopsIterativeScanQueryBuilder.class)
public class AzureDevopsIterativeScanQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date from;

    @JsonProperty("to")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date to;

    @JsonProperty("fetchOnce")
    Boolean fetchOnce;

    @JsonProperty("fetch_all_iterations")
    Boolean fetchAllIterations;

    @JsonProperty("fetch_metadata")
    Boolean fetchMetadata;

    @JsonProperty("ingestion_flags")
    Map<String, Object> ingestionFlags;

    @JsonProperty("job_category")
    JobCategory jobCategory;

}