package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = InnerBucket.InnerBucketBuilder.class)
public class InnerBucket {
    /*
                      "key_as_string": "2023-01",
                  "key": 1672531200000,
                  "doc_count": 85
     */
    @JsonProperty("key_as_string")
    private final String key_as_string;
    @JsonProperty("key")
    private final Long key;
    @JsonProperty("doc_count")
    private final Integer doc_count;
}
