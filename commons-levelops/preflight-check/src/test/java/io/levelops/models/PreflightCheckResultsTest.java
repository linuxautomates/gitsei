package io.levelops.models;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PreflightCheckResultsTest {

    @Test
    public void testAllChecksMustPass() {

        PreflightCheckResult failedCheck = PreflightCheckResult.builder()
                .success(false)
                .build();
        PreflightCheckResult successCheck = PreflightCheckResult.builder()
                .success(true)
                .build();

        // defaults to success
        assertSuccess(PreflightCheckResults.builder().build());

        // fails if if root has failed
        assertFailure(PreflightCheckResults.builder()
                .success(false)
                .build());

        // success if check has failed but root is not explicitly
        // set to failed and allChecksMustPass not specified
        assertSuccess(PreflightCheckResults.builder()
                .check(failedCheck)
                .build());

        // fails if allCheckMustPass and at least 1 check has failed
        assertFailure(PreflightCheckResults.builder()
                .allChecksMustPass()
                .check(successCheck)
                .check(failedCheck)
                .build());

        // success if allChecksMustPass and all checks passed
        assertSuccess(PreflightCheckResults.builder()
                .allChecksMustPass()
                .check(successCheck)
                .check(successCheck)
                .build());

        // failure if root has failed despite all check passed
        assertFailure(PreflightCheckResults.builder()
                .success(false)
                .allChecksMustPass()
                .check(successCheck)
                .check(successCheck)
                .build());
    }

    private void assertSuccess(PreflightCheckResults results) {
        assertThat(results.isSuccess()).isTrue();
    }

    private void assertFailure(PreflightCheckResults results) {
        assertThat(results.isSuccess()).isFalse();
    }
}