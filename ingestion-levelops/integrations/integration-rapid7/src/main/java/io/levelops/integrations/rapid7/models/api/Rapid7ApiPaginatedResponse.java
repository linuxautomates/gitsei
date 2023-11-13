package io.levelops.integrations.rapid7.models.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.util.List;

//@Value
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Rapid7ApiPaginatedResponse<T> {

    @JsonProperty("data")
    List<T> data;
    @JsonProperty("metadata")
    Metadata metadata;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Metadata.MetadataBuilder.class)
    public static class Metadata {
        @JsonProperty("index")
        Integer index;
        @JsonProperty("size")
        Integer size;
        @JsonProperty("total_pages")
        Integer totalPages;
        @JsonProperty("total_data")
        Integer totalData;
    }
}
