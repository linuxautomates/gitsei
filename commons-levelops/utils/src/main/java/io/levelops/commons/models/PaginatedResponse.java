package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Model wrapping a page of data to include pagination metadata.
 *
 * @param <T>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class PaginatedResponse<T> {

    @JsonUnwrapped
    private ListResponse<T> response;

    @JsonProperty("_metadata")
    private Metadata metadata;

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(builder = Metadata.MetadataBuilder.class)
    public static class Metadata {
        @JsonProperty("page_size")
        Integer pageSize;
        @JsonProperty("page")
        Integer page;
        @JsonProperty("next_page")
        Integer nextPage;
        @JsonProperty("has_next")
        Boolean hasNext;
        @JsonProperty("total_count")
        Integer totalCount;
        @JsonProperty("calculated_at")
        Long calculatedAt;
    }

    public static <T> PaginatedResponse<T> of(int pageNumber, int pageSize, DbListResponse<T> listResponse) {
        if (listResponse == null) {
            return PaginatedResponse.of(pageNumber, pageSize, 0, Collections.emptyList());
        }

        if (listResponse.getCalculatedAt() != null) {
            return PaginatedResponse.of(pageNumber, pageSize, listResponse.getTotalCount(), listResponse.getCalculatedAt(), listResponse.getRecords());
        }

        return PaginatedResponse.of(pageNumber, pageSize, listResponse.getTotalCount(), listResponse.getRecords());
    }

    public static <T> PaginatedResponse<T> of(int pageNumber, int pageSize, @Nullable List<T> data) {
        return PaginatedResponse.of(pageNumber, pageSize, null, data);
    }

    public static <T> PaginatedResponse<T> of(int pageNumber, int pageSize, Integer totalCount, @Nullable List<T> data) {
        data = ObjectUtils.defaultIfNull(data, Collections.emptyList());
        boolean lastPage = data.size() < pageSize;
        return PaginatedResponse.<T>builder()
                .metadata(Metadata.builder()
                        .page(pageNumber)
                        .pageSize(pageSize)
                        .nextPage(lastPage ? null : pageNumber + 1)
                        .hasNext(!lastPage)
                        .totalCount(totalCount)
                        .build())
                .response(ListResponse.of(data))
                .build();
    }

    public static <T> PaginatedResponse<T> of(int pageNumber, int pageSize, Integer totalCount, Long calculatedAt, @Nullable List<T> data) {
        data = ObjectUtils.defaultIfNull(data, Collections.emptyList());
        boolean lastPage = data.size() < pageSize;
        return PaginatedResponse.<T>builder()
                .metadata(Metadata.builder()
                        .page(pageNumber)
                        .pageSize(pageSize)
                        .nextPage(lastPage ? null : pageNumber + 1)
                        .hasNext(!lastPage)
                        .calculatedAt(calculatedAt)
                        .totalCount(totalCount)
                        .build())
                .response(ListResponse.of(data))
                .build();
    }

    public static <T> JavaType typeOf(ObjectMapper objectMapper, Class<T> parameterType) {
        return objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, parameterType);
    }

}
