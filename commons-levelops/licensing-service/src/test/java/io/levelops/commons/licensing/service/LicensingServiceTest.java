package io.levelops.commons.licensing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LicensingServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deserializeLicense() throws IOException {

        License license = mapper.readValue(ResourceUtils.getResourceAsString("json/licensing-foo.json"), License.class);

        assertThat(license.getLicense()).isEqualTo("full");
        assertThat(license.getCompany()).isEqualTo("foo");
        assertThat(license.getEntitlements().size()).isEqualTo(1);
        assertThat(license.getEntitlements().get(0)).isEqualTo("ALL_FEATURES");
        assertThat(license.getCreatedAt()).isEqualTo(1648800993);
        assertThat(license.getUpdatedAt()).isEqualTo(1648800993);

        license = mapper.readValue(ResourceUtils.getResourceAsString("json/licensing-demo.json"), License.class);

        assertThat(license.getLicense()).isEqualTo("limited_trial");
        assertThat(license.getCompany()).isEqualTo("demo");
        assertThat(license.getEntitlements().size()).isEqualTo(4);
        assertThat(license.getEntitlements()).hasSameElementsAs(List.of("DASHBOARDS", "PROPELS_COUNT_5", "SETTINGS_SCM_INTEGRATIONS_COUNT_1", "SETTINGS_ORG_UNITS"));
        assertThat(license.getCreatedAt()).isEqualTo(1648800993);
        assertThat(license.getUpdatedAt()).isEqualTo(1648800993);

    }
}
