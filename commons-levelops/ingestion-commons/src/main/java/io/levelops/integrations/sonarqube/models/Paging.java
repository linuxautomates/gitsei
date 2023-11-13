package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Paging.PagingBuilder.class)
public class Paging {

    @JsonProperty("pageIndex")
    long pageIndex;

    @JsonProperty("pageSize")
    long pageSize;

    @JsonProperty("total")
    long total;

}
