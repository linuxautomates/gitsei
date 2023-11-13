package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.levelops.commons.licensing.model.LicenseType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * NOTE: This class uses Jackson to serialize to a CSV file instead of JSON. So if you see any surprising naming
 * conventions, you know why!
 */
@Log4j2
@Data
@Builder(toBuilder = true)
@JsonPropertyOrder({"tenantName", "licenseType", "lastLoggedInUser", "loginTimeEpochSeconds"})
public class TenantActivityReport  implements Comparable<TenantActivityReport>{
    @JsonProperty("Tenant Name")
    private String tenantName;

    @JsonProperty("License Type")
    private LicenseType licenseType;

    @JsonProperty("Last logged in user")
    private String lastLoggedInUser;

    @JsonProperty("Last login time (PT)")
    private Long loginTimeEpochSeconds;

    @JsonIgnore
    private String tenantId;

    @JsonGetter("Last logged in user")
    public String getSerializedLastLoggedInUser() {
        if (this.lastLoggedInUser == null || this.lastLoggedInUser.isEmpty()) {
            return "<No logged in user>";
        }
        return this.lastLoggedInUser;
    }

    @JsonGetter("Last login time (PT)")
    public String getSerializedLoginTime() {
        if (this.loginTimeEpochSeconds == null || this.lastLoggedInUser == null) {
            return "";
        }
        return LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(this.loginTimeEpochSeconds), ZoneId.of("America/Los_Angeles"))
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public int compareTo(@NotNull TenantActivityReport o) {
        var order = List.of(
                LicenseType.LIMITED_TRIAL_LICENSE,
                LicenseType.FULL_LICENSE,
                LicenseType.UNKNOWN
        );
        var thisIdx = order.indexOf(this.getLicenseType());
        var otherIdx = order.indexOf(o.getLicenseType());
        if (thisIdx != otherIdx) {
            // Limited trial customers on top of the report
            return Integer.compare(thisIdx, otherIdx);
        } else {
            if (Objects.equals(this.getLoginTimeEpochSeconds(), o.getLoginTimeEpochSeconds())) {
                return 0;
            }
            // Secondary sort by login time
            // Dump customers with no login time at bottom
            if (this.loginTimeEpochSeconds == null) {
                return 1;
            }
            if (o.loginTimeEpochSeconds == null) {
                return -1;
            }
            return this.loginTimeEpochSeconds.compareTo(o.loginTimeEpochSeconds);
        }
    }

    public static String toXml(List<TenantActivityReport> reports) throws IOException {
        CsvMapper csvMapper = CsvMapper.builder()
                .enable(CsvGenerator.Feature.STRICT_CHECK_FOR_QUOTING)
                .build();
        var schema = csvMapper
                .schemaFor(TenantActivityReport.class)
                .withHeader();
        try (StringWriter strW = new StringWriter()) {
            SequenceWriter seqW = csvMapper.writer(schema).writeValues(strW);
            for (TenantActivityReport report : reports) {
                seqW.write(report);
            }
            log.info(strW.toString());
            return strW.toString();
        }
    }
}