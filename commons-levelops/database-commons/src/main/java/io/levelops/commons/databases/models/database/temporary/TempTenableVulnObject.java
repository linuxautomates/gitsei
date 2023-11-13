package io.levelops.commons.databases.models.database.temporary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.tenable.models.NetworkVulnerability;
import io.levelops.integrations.tenable.models.WasVulnerability;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class TempTenableVulnObject {
    private static final DateFormat networkDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final DateFormat wasDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @JsonProperty("id") //unique id
    private String id;

    @JsonProperty("vuln_type")
    private VulnType vulnType;

    @JsonProperty("vuln_desciption")
    private String vulnDescription;

    @JsonProperty("vuln_id")
    private String vulnId;

    @JsonProperty("vuln_name")
    private String vulnName;

    @JsonProperty("vuln_severity")
    private String vulnSeverity;

    @JsonProperty("ips")
    private List<String> ips;

    @JsonProperty("fqdns")
    private List<String> fqdns;

    @JsonProperty("cvss")
    private String cvss;

    @JsonProperty("cvss3")
    private String cvss3;

    @JsonProperty("cve")
    private List<String> cve;

    @JsonProperty("asset_name")
    private String assetName;

    @JsonProperty("asset_os")
    private List<String> assetOS;

    @JsonProperty("asset_vuln_status")
    private String assetVulnStatus;

    @JsonProperty("asset_vuln_first_found")
    private Long assetVulnFirstFound;

    @JsonProperty("asset_vuln_last_found")
    private Long assetVulnLastFound;

    @JsonProperty("asset_vuln_last_fixed")
    private Long assetVulnLastFixed;

    public static TempTenableVulnObject fromWasVuln(WasVulnerability source) {
        try {
            TempTenableVulnObjectBuilder tempTenableVulnObjectBuilder = TempTenableVulnObject.builder()
                    .vulnType(VulnType.WAS)
                    .id(source.getVulnId())
                    .vulnDescription(source.getDetails().getOutput())
                    .ips(Collections.emptyList())
                    .fqdns(Collections.singletonList(source.getUri()))
                    .assetName(source.getUri())
                    .vulnSeverity("UNDEFINED")
                    .assetVulnStatus("UNDEFINED")
                    .assetVulnLastFound(wasDateFormat.parse(source.getCreatedAt()).toInstant().getEpochSecond())
                    .assetVulnFirstFound(wasDateFormat.parse(source.getCreatedAt()).toInstant().getEpochSecond());
            return tempTenableVulnObjectBuilder.build();
        } catch (ParseException e) {
            log.warn("Failed to parse network vuln. ", e);
        }
        return null;
    }

    public static TempTenableVulnObject fromNetworkVuln(NetworkVulnerability source) {
        TempTenableVulnObjectBuilder tempTenableVulnObjectBuilder = null;
        try {
            tempTenableVulnObjectBuilder = TempTenableVulnObject.builder()
                    .vulnType(VulnType.NETWORK)
                    .id(UUID.randomUUID().toString())
                    .vulnDescription(source.getPlugin().getDescription())
                    .ips(Collections.singletonList(source.getAsset().getIpv6()))
                    .fqdns(Collections.singletonList(source.getAsset().getFqdn()))
                    .cvss(source.getPlugin().getCvssBaseScore())
                    .cvss3(source.getPlugin().getCvss3BaseScore())
                    .cve(source.getPlugin().getCve())
                    .assetName(source.getAsset().getHostname())
                    .vulnSeverity(source.getSeverity().toUpperCase())
                    .assetVulnStatus(source.getState().toUpperCase())
                    .assetOS(source.getAsset().getOperatingSystem())
                    .assetVulnLastFound(networkDateFormat.parse(source.getLastFound()).toInstant().getEpochSecond())
                    .assetVulnFirstFound(networkDateFormat.parse(source.getFirstFound()).toInstant().getEpochSecond());
            if (source.getLastFixed() != null) {
                tempTenableVulnObjectBuilder.assetVulnLastFixed(networkDateFormat.parse(source.getLastFixed()).toInstant().getEpochSecond());
            }
            return tempTenableVulnObjectBuilder.build();
        } catch (ParseException e) {
            log.warn("Failed to parse network vuln. ", e);
        }
        return null;
    }

    public enum VulnType {
        WAS, //web application scan
        NETWORK //network scan
    }
}