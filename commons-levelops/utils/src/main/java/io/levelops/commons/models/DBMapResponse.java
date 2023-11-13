package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Map;

@Getter
@Builder(toBuilder = true)
// @NoArgsConstructor
// @AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBMapResponse<T,D>{
    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("records")
    @Singular
    private Map<T,D> records;

    public static <T,D> DBMapResponse<T,D> of(Map<T,D> records, Integer totalCount) {
        return DBMapResponse.<T,D>builder()
                .records(records)
                .totalCount(totalCount)
                .build();
    }

    @JsonProperty("count")
    public Integer getCount() {
        return (records == null) ? 0 : records.size();
    }
}
