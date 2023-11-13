package io.levelops.aggregations.services;

import io.levelops.aggregations.models.TenantActivityReport;
import io.levelops.commons.licensing.model.LicenseType;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Log4j2
public class TenantActivityReportTest {

    @Test
    public void xmlSerializationTest() throws IOException {
        Instant now = Instant.now();
        String nowStr = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(now.getEpochSecond()),
                ZoneId.of("America/Los_Angeles")).format(DateTimeFormatter.ISO_DATE_TIME);
        var report1 = TenantActivityReport.builder()
                .tenantId("sid")
                .tenantName("SidTenant")
                .licenseType(LicenseType.FULL_LICENSE)
                .lastLoggedInUser("sid@gmail.com")
                .loginTimeEpochSeconds(now.getEpochSecond())
                .build();

        var report2 = TenantActivityReport.builder()
                .tenantId("sid")
                .tenantName("SidTenant")
                .licenseType(LicenseType.FULL_LICENSE)
                .build();

        var xmlStr = TenantActivityReport.toXml(
                List.of(report1, report2)
        );
        assertThat(xmlStr).isEqualTo(
                "Tenant Name,License Type,Last logged in user,Last login time (PT)\n" +
                        "SidTenant,full,sid@gmail.com," + nowStr + "\n" +
                        "SidTenant,full,<No logged in user>,\n"
        );
    }
}
