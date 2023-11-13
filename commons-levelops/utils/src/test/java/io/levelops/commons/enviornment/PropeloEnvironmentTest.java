package io.levelops.commons.enviornment;

import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PropeloEnvironmentTest {
   @Test
   public void testSerialization() {
       assertThat(PropeloEnvironmentType.ASIA1.toString()).isEqualTo("asia1");
       assertThat(PropeloEnvironmentType.PROD.toString()).isEqualTo("prod");
       assertThat(PropeloEnvironmentType.DEV.toString()).isEqualTo("dev");
       assertThat(PropeloEnvironmentType.STAGING.toString()).isEqualTo("staging");
       assertThat(PropeloEnvironmentType.HARNESS_QA.toString()).isEqualTo("qa-setup");
       assertThat(PropeloEnvironmentType.HARNESS_PRE_QA.toString()).isEqualTo("preqa-setup");
       assertThat(PropeloEnvironmentType.HARNESS_PROD.toString()).isEqualTo("prod-setup");
   }


   @Test
    public void testIsProd() {
       assertThat(PropeloEnvironmentType.ASIA1.isProd()).isTrue();
       assertThat(PropeloEnvironmentType.PROD.isProd()).isTrue();
       assertThat(PropeloEnvironmentType.DEV.isProd()).isFalse();
       assertThat(PropeloEnvironmentType.STAGING.isProd()).isFalse();
       assertThat(PropeloEnvironmentType.HARNESS_PROD.isProd()).isTrue();
       assertThat(PropeloEnvironmentType.HARNESS_QA.isProd()).isFalse();
       assertThat(PropeloEnvironmentType.HARNESS_PRE_QA.isProd()).isFalse();
   }
}