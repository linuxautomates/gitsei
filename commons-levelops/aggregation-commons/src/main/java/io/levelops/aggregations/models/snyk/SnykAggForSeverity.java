package io.levelops.aggregations.models.snyk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@NoArgsConstructor
public class SnykAggForSeverity {
    @JsonProperty("vulns_found")
    private Integer vulns = 0;
    @JsonProperty("vulns_suppressed")
    private Integer suppressed = 0;
    @JsonProperty("vulns_patched")
    private Integer patched = 0;
    @JsonProperty("vulns_suppressed_wont_fix")
    private Integer wontFixSuppress = 0;
    @JsonProperty("vulns_suppressed_not_vulnerable")
    private Integer notVulnerableSuppress = 0;
    @JsonProperty("vulns_suppressed_temporary")
    private Integer temporarySuppress = 0;
    @JsonProperty("vulns_suppressed_custom")
    private Integer customSuppress = 0;
}
