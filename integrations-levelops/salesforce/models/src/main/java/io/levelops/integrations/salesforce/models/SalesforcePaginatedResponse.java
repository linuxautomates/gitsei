package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SalesforcePaginatedResponse.SalesforcePaginatedResponseBuilder.class)
public class SalesforcePaginatedResponse<T> {

    @JsonProperty("Sforce-Locator")
    String salesForceLocator;

    @JsonProperty
    List<T> records;
}
