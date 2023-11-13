package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class ApiKeyToken {
    @JsonProperty(value = "key")
    private String key;
    @JsonProperty(value = "id")
    private String id;
    @JsonProperty(value = "company")
    private String company;
}
