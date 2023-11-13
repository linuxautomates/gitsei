package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Request parameters for fetching vulnerabilities. For more information check parameters of <a href="https://developer.tenable.com/reference#exports-vulns-request-export</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VulnerabilitiesExportRequest.VulnerabilitiesExportRequestBuilder.class)
public class VulnerabilitiesExportRequest {
    @JsonProperty("num_assets")
    Integer numAssets;

    @JsonProperty("filters")
    Filter filters;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Filter.FilterBuilder.class)
    public static class Filter {

        @JsonProperty
        Long since;
    }
}
