package io.levelops.api.utils;

import io.levelops.web.exceptions.ForbiddenException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SelfServeEndpointUtilsTest {

    @Test
    public void validateUser() throws ForbiddenException {
        assertThatThrownBy(() -> SelfServeEndpointUtils.validateUser("user@foo.io")).isInstanceOf(ForbiddenException.class);

        SelfServeEndpointUtils.validateUser("user@harness.io");
        SelfServeEndpointUtils.validateUser("user@levelops.io");
        SelfServeEndpointUtils.validateUser("user@propelo.ai");
    }
}