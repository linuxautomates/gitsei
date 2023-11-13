package io.levelops.commons.licensing.model;


import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LicenseTypeTest {
    @Test
    public void serializationTest() {
        assertThat(LicenseType.FULL_LICENSE.toString()).isEqualTo("full");
        assertThat(LicenseType.LIMITED_TRIAL_LICENSE.toString()).isEqualTo("limited_trial");
        assertThat(LicenseType.UNKNOWN.toString()).isEqualTo("UNKNOWN");
    }

    @Test
    public void deserializationTest() {
        assertThat(LicenseType.fromString("full")).isEqualTo(LicenseType.FULL_LICENSE);
        assertThat(LicenseType.fromString("limited_trial")).isEqualTo(LicenseType.LIMITED_TRIAL_LICENSE);
        assertThat(LicenseType.fromString("UNKNOWN")).isEqualTo(LicenseType.UNKNOWN);
        assertThat(LicenseType.fromString("junk")).isEqualTo(LicenseType.UNKNOWN);
    }

}