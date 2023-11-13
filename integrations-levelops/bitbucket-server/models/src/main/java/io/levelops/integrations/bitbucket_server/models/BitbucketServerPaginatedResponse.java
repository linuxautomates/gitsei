package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class BitbucketServerPaginatedResponse<T> {

    @JsonProperty("size")
    Integer size;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("isLastPage")
    Boolean isLastPage;

    @JsonProperty("start")
    Integer currentPageStart;

    @JsonProperty("nextPageStart")
    Integer nextPageStart;

    @JsonProperty("values")
    List<T> values;

    public static <T> JavaType ofType(ObjectMapper objectMapper, Class<T> clazz) {
        return objectMapper.getTypeFactory().constructParametricType(BitbucketServerPaginatedResponse.class, clazz);
    }
}
