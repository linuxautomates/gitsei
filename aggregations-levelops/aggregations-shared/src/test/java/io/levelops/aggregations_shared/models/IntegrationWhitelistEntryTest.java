package io.levelops.aggregations_shared.models;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationWhitelistEntryTest {
    @Test
    public void testParse() {
        var parsed = IntegrationWhitelistEntry.fromCommaSeparatedString("harnessng::1234");
        var parsedEmpty = IntegrationWhitelistEntry.fromCommaSeparatedString("");
        var parsedNull = IntegrationWhitelistEntry.fromCommaSeparatedString(null);
        assertThat(parsed).containsExactly(IntegrationWhitelistEntry.builder()
                .tenantId("harnessng")
                .integrationId("1234")
                .build());
        assertThat(parsedEmpty).isNotNull();
        assertThat(parsedEmpty).isEmpty();
        assertThat(parsedNull).isNotNull();
        assertThat(parsedNull).isEmpty();
    }
}
