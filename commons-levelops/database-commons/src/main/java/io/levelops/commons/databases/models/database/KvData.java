package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KvData {

    @JsonProperty("key")
    private String key;

    @JsonProperty("values")
    private List<Value> values;

    /**
     * Content type of the values.
     * ALL the values must be of the same type!
     * This class can be wrapped by {@link io.levelops.commons.models.ContentType}
     */
    @JsonProperty("type")
    private String type;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public static class Value {
        @JsonProperty("value")
        private String value;
        @JsonProperty("type")
        private String type;
    }

}
