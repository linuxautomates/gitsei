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

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccessCheckResponseDTO {
    @JsonProperty("status")
    String status;
    @JsonProperty("code")
    String code;
    @JsonProperty("message")
    String message;
    @JsonProperty("data")
    AccessCheckDataResponse accessCheckDataResponse;
    @JsonProperty("metadata")
    ResponseMetadata metadata;
    @JsonProperty("correlationId")
    String correlationId;
    @JsonProperty("responseMessages")
    List<ResponseMessages> responseMessages;
    @JsonProperty("detailedMessage")
    String detailedMessage;
    @JsonProperty("errors")
    List<ResponseErrors> responseErrors;

}
