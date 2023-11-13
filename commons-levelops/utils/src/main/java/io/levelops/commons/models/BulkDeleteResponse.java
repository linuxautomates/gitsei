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
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkDeleteResponse {

    @JsonProperty("records")
    @Singular
    private List<DeleteResponse> records;

    @JsonProperty("count")
    public int getCount() {
        return (records == null) ? 0 : records.size();
    }

    @JsonCreator
    public BulkDeleteResponse(@JsonProperty("records") List<DeleteResponse> records) {
        this.records = records;
    }

    public static BulkDeleteResponse of(List<DeleteResponse> records) {
        return BulkDeleteResponse.builder()
                .records(records)
                .build();
    }

    public static <T> JavaType typeOf(ObjectMapper objectMapper, Class<T> parameterType) {
        return objectMapper.getTypeFactory().constructParametricType(BulkDeleteResponse.class, parameterType);
    }

    public static BulkDeleteResponse createBulkDeleteResponse(List<String> templateIds, Boolean success, String error) {
        List<DeleteResponse> responses = templateIds
                .stream()
                .filter(Objects::nonNull)
                .map(id -> (DeleteResponse.builder().id(id).success(success).error(error).build()))
                .collect(Collectors.toList());
        return BulkDeleteResponse.of(responses);
    }


}
