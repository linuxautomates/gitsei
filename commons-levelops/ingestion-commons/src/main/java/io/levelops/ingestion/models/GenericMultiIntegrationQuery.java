package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GenericMultiIntegrationQuery.GenericMultiIntegrationQueryBuilder.class)
public class GenericMultiIntegrationQuery implements DataQuery {

    @JsonProperty("queries")
    List<GenericIntegrationQuery> queries;

}