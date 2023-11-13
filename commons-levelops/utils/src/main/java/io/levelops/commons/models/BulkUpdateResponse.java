package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkUpdateResponse {
    @JsonProperty("records")
    @Singular
    private final List<UpdateResponse> records;

    @JsonProperty("count")
    public int getCount() {
        return (records == null) ? 0 : records.size();
    }

    @JsonCreator
    public BulkUpdateResponse(@JsonProperty("records") List<UpdateResponse> records) {
        this.records = records;
    }

    public static BulkUpdateResponse of(List<UpdateResponse> records) {
        return BulkUpdateResponse.builder()
                .records(records)
                .build();
    }

    public static <T> JavaType typeOf(ObjectMapper objectMapper, Class<T> parameterType) {
        return objectMapper.getTypeFactory().constructParametricType(BulkUpdateResponse.class, parameterType);
    }

    public static BulkUpdateResponse createBulkUpdateResponse(Stream<String> idsStream, Boolean success, String error) {
        List<UpdateResponse> responses = idsStream
                .filter(Objects::nonNull)
                .map(id -> (UpdateResponse.builder().id(id).success(success).error(error).build()))
                .collect(Collectors.toList());
        return BulkUpdateResponse.of(responses);
    }
}