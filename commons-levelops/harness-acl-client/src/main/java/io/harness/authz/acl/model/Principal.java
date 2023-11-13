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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Principal {
    @JsonProperty("principalIdentifier")
    String principalIdentifier;
    @JsonProperty("principalType")
    PrincipalType principalType;

    public static Principal of(PrincipalType principalType, String principalIdentifier) {
        return Principal.builder().principalIdentifier(principalIdentifier).principalType(principalType).build();
    }
}