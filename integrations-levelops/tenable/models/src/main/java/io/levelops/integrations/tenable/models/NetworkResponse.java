package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for Networks list response <a href="https://developer.tenable.com/reference#networks-create</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = NetworkResponse.NetworkResponseBuilder.class)
public class NetworkResponse {
    @JsonProperty
    List<Network> networks;

    @JsonProperty
    Pagination pagination;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Network.NetworkBuilder.class)
    public static class Network {
        @JsonProperty("owner_uuid")
        String ownerUUID;

        @JsonProperty
        Long created;

        @JsonProperty
        Long modified;

        @JsonProperty
        String uuid;

        @JsonProperty
        String name;

        @JsonProperty
        String description;

        @JsonProperty("is_default")
        Boolean isDefault;

        @JsonProperty("created_by")
        String createdBy;

        @JsonProperty("modified_by")
        String modifiedBy;

        @JsonProperty("deleted_at")
        Long deletedAt;

        @JsonProperty("created_in_seconds")
        Long createdInSeconds;

        @JsonProperty("modified_in_seconds")
        Long modifiedInSeconds;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Pagination.PaginationBuilder.class)
    public static class Pagination {

        @JsonProperty
        Integer total;

        @JsonProperty
        Integer limit;

        @JsonProperty
        Integer offset;

        @JsonProperty
        List<Sort> sort;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Sort.SortBuilder.class)
    public static class Sort {

        @JsonProperty
        String name;

        @JsonProperty
        String order;
    }
}
