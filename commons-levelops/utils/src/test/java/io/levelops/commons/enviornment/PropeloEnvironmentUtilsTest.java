package io.levelops.commons.enviornment;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PropeloEnvironmentUtilsTest {
    @Test
    public void testGetEnvironmentFromOauthUrl() {
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://asia1.app.propelo.ai"))
                .isEqualTo(PropeloEnvironmentType.ASIA1);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://app.propelo.ai"))
                .isEqualTo(PropeloEnvironmentType.PROD);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://testui1.propelo.ai"))
                .isEqualTo(PropeloEnvironmentType.DEV);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://staging.app.propelo.ai"))
                .isEqualTo(PropeloEnvironmentType.STAGING);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://qa.harness.io"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_QA);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://stress.harness.io"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_PRE_QA);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://app.harness.io"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_PROD);

    }


    @Test(expected = IllegalArgumentException.class)
    public void testGetEnvironmentOauthUrlException() {
        var result = PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl("https://junk-url.haha");
    }

    @Test
    public void testGetEnvironmentFromAggProject() {
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("levelops-asia1"))
                .isEqualTo(PropeloEnvironmentType.ASIA1);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("levelops-api-and-data"))
                .isEqualTo(PropeloEnvironmentType.PROD);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("levelops-dev"))
                .isEqualTo(PropeloEnvironmentType.DEV);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("levelops-staging"))
                .isEqualTo(PropeloEnvironmentType.STAGING);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("qa-setup"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_QA);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("prod-setup"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_PROD);
        assertThat(PropeloEnvironmentUtils.getEnvironmentFromAggProject("preqa-setup"))
                .isEqualTo(PropeloEnvironmentType.HARNESS_PRE_QA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEnvironmentAggProjectException() {
        var result = PropeloEnvironmentUtils.getEnvironmentFromAggProject("junk");
    }
}