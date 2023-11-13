package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CiCdJobQualifiedName {
    @JsonProperty("instance_name")
    private String instanceName;
    @JsonProperty("job_name")
    private String jobName;
}
