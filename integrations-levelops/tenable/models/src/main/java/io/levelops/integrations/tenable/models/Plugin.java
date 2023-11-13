package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Plugin.PluginBuilder.class)
public class Plugin {
    @JsonProperty
    List<Integer> bid;

    @JsonProperty("canvas_package")
    String canvasPackage;

    @JsonProperty("checks_for_default_account")
    Boolean checksForDefaultAccount;

    @JsonProperty("checks_for_malware")
    Boolean checksForMalware;

    @JsonProperty
    List<String> cpe;

    @JsonProperty
    List<String> cve;

    @JsonProperty("cvss3_base_score")
    String cvss3BaseScore;

    @JsonProperty("cvss3_temporal_score")
    String cvss3TemporalScore;

    @JsonProperty("cvss3_temporal_vector")
    CVSS3TemporalVector cvss3TemporalVector;

    @JsonProperty("cvss3_vector")
    CVSS3Vector cvss3Vector;

    @JsonProperty("cvss_base_score")
    String cvssBaseScore;

    @JsonProperty("cvss_temporal_score")
    String cvssTemporalScore;

    @JsonProperty("cvss_temporal_vector")
    CVSSTemporalVector cvssTemporalVector;

    @JsonProperty("cvss_vector")
    CVSSVector cvssVector;

    @JsonProperty("d2_elliot_name")
    String d2ElliotName;

    @JsonProperty
    String description;

    @JsonProperty("exploit_available")
    Boolean exploitAvailable;

    @JsonProperty("exploit_framework_canvas")
    Boolean exploitFrameworkCanvas;

    @JsonProperty("exploit_framework_core")
    Boolean exploitFrameworkCore;

    @JsonProperty("exploit_framework_d2_elliot")
    Boolean exploitFrameworkD2Elliot;

    @JsonProperty("exploit_framework_exploithub")
    Boolean exploitFrameworkExploithub;

    @JsonProperty("exploit_framework_metasploit")
    Boolean exploitFrameworkMetasploit;

    @JsonProperty("exploitability_ease")
    String exploitabilityEase;

    @JsonProperty("exploited_by_malware")
    Boolean exploitedByMalware;

    @JsonProperty("exploited_by_nessus")
    Boolean exploitedByNessus;

    @JsonProperty("exploithub_sku")
    String exploithubSku;

    @JsonProperty
    String family;

    @JsonProperty("family_id")
    Integer familyId;

    @JsonProperty("has_patch")
    Boolean hasPatch;

    @JsonProperty
    Integer id;

    @JsonProperty("in_the_news")
    Boolean inTheNews;

    @JsonProperty("metasploit_name")
    String metasploitName;

    @JsonProperty("ms_bulletin")
    String msBulletin;

    @JsonProperty
    String name;

    @JsonProperty("patch_publication_date")
    String patchPublicationDate;

    @JsonProperty("modification_date")
    String modificationDate;

    @JsonProperty("publication_date")
    String publicationDate;

    @JsonProperty("risk_factor")
    String riskFactor;

    @JsonProperty("see_also")
    List<String> seeAlso;

    @JsonProperty
    String solution;

    @JsonProperty("stig_severity")
    String stigSeverity;

    @JsonProperty
    String synopsis;

    @JsonProperty
    String type;

    @JsonProperty("unsupported_by_vendor")
    Boolean unsupportedByVendor;

    @JsonProperty
    String usn;

    @JsonProperty
    String version;

    @JsonProperty("vuln_publication_date")
    String vulnPublicationDate;

    @JsonProperty
    List<XRefs> xrefs;

    @JsonProperty
    VPR vpr;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CVSS3TemporalVector.CVSS3TemporalVectorBuilder.class)
    public static class CVSS3TemporalVector {

        @JsonProperty("Exploitability")
        String exploitability;

        @JsonProperty("RemediationLevel")
        String remediationLevel;

        @JsonProperty("ReportConfidence")
        String reportConfidence;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CVSS3Vector.CVSS3VectorBuilder.class)
    public static class CVSS3Vector {

        @JsonProperty("AccessComplexity")
        String accessComplexity;

        @JsonProperty("AccessVector")
        String accessVector;

        @JsonProperty("Authentication")
        String authentication;

        @JsonProperty("Availability-Impact")
        String availabilityImpact;

        @JsonProperty("Confidentiality-Impact")
        String confidentialityImpact;

        @JsonProperty("Integrity-Impact")
        String integrityImpact;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CVSSTemporalVector.CVSSTemporalVectorBuilder.class)
    public static class CVSSTemporalVector {

        @JsonProperty("Exploitability")
        String exploitability;

        @JsonProperty("RemediationLevel")
        String remediationLevel;

        @JsonProperty("ReportConfidence")
        String reportConfidence;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CVSSVector.CVSSVectorBuilder.class)
    public static class CVSSVector {

        @JsonProperty("AccessComplexity")
        String accessComplexity;

        @JsonProperty("AccessVector")
        String accessVector;

        @JsonProperty("Authentication")
        String authentication;

        @JsonProperty("Availability-Impact")
        String availabilityImpact;

        @JsonProperty("Confidentiality-Impact")
        String confidentialityImpact;

        @JsonProperty("Integrity-Impact")
        String integrityImpact;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = VPR.VPRBuilder.class)
    public static class VPR {
        @JsonProperty
        Integer score;

        @JsonProperty
        Driver drivers;

        @JsonProperty
        String updated;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = XRefs.XRefsBuilder.class)
    public static class XRefs {
        @JsonProperty
        String type;

        @JsonProperty
        String id;
    }
}
