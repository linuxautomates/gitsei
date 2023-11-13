package io.harness.authz.acl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceScope {
    @JsonProperty("accountIdentifier")
    private String accountIdentifier;
    @JsonProperty("orgIdentifier")
    private String orgIdentifier;
    @JsonProperty("projectIdentifier")
    private String projectIdentifier;

    public static ResourceScope NONE = ResourceScope.of(null, null, null);

    public static ResourceScope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
        return ResourceScope.builder()
                .accountIdentifier(accountIdentifier)
                .orgIdentifier(orgIdentifier)
                .projectIdentifier(projectIdentifier)
                .build();
    }
}