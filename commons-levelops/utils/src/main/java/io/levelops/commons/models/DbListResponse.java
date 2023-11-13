package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbListResponse<T> {
    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("records")
    @Singular
    private List<T> records;

    @JsonProperty("totals")
    private Map<String, Object> totals;

    @JsonProperty("calculated_at")
    private Long calculatedAt;

    public static <T> DbListResponse<T> of(List<T> records, Integer totalCount) {
        return DbListResponse.<T>builder()
                .records(records != null ? records : Collections.emptyList())
                .totalCount(totalCount)
                .build();
    }

    public static <T> DbListResponse<T> of(List<T> records, Integer totalCount, Map<String, Object> totals) {
        return DbListResponse.<T>builder()
                .records(records != null ? records : Collections.emptyList())
                .totalCount(totalCount)
                .totals(totals)
                .build();
    }

    public static <T> DbListResponse<T> of(List<T> records, Integer totalCount, Long calculatedAt) {
        return DbListResponse.<T>builder()
                .records(records != null ? records : Collections.emptyList())
                .totalCount(totalCount)
                .calculatedAt(calculatedAt)
                .build();
    }

    @JsonProperty("count")
    public Integer getCount() {
        return (records == null) ? 0 : records.size();
    }

    public static <T> JavaType typeOf(ObjectMapper objectMapper, Class<T> parameterType) {
        return objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, parameterType);
    }

}
