package io.levelops.commons.databases.models.database.mappings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductIntegMapping {

    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "product_id")
    private String productId;

    @JsonProperty(value = "integration_id")
    private String integrationId;

    @Builder.Default
    @JsonProperty(value = "mappings")
    private Map<String, Object> mappings = Collections.emptyMap();

    @JsonProperty(value = "updated_at")
    private Long updatedAt;

    @JsonProperty(value = "created_at")
    private Long createdAt;

}
