package io.harness.authz.acl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessControlDTO {
    @JsonProperty("permission")
    String permission;
    @JsonProperty("resourceScope")
    ResourceScope resourceScope;
    @JsonProperty("resourceType")
    String resourceType;
    @JsonProperty("resourceAttributes")
    Map<String, String> resourceAttributes;
    @JsonProperty("resourceIdentifier")
    String resourceIdentifier;
    @JsonProperty("permitted")
    boolean permitted;
}
