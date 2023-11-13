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

/**
 * Model to represent a list of records.
 *
 * @param <T>
 */
@Getter
@Builder
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListResponse<T> {

    @JsonProperty("records")
    @Singular
    private List<T> records;

    @JsonProperty("count")
    public int getCount() {
        return (records == null) ? 0 : records.size();
    }

    @JsonCreator
    public ListResponse(@JsonProperty("records") List<T> records) {
        this.records = records;
    }

    public static <T> ListResponse<T> of(List<T> records) {
        return ListResponse.<T>builder()
                .records(records)
                .build();
    }

    public static <T> JavaType typeOf(ObjectMapper objectMapper, Class<T> parameterType) {
        return objectMapper.getTypeFactory().constructParametricType(ListResponse.class, parameterType);
    }

}
