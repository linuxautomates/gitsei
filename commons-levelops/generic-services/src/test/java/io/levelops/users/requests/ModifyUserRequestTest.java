package io.levelops.users.requests;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ModifyUserRequestTest {

    @Test
    public void test() throws JsonProcessingException {
        Instant date = Instant.parse("2022-05-20T21:40:12.000Z");
        long epoch = 1653082812;
        assertThat(date.getEpochSecond()).isEqualTo(epoch);

        ModifyUserRequest modifyUserRequest = new ModifyUserRequest(null, null, null, null, null, null, null, null, null,  epoch, epoch, null);
        assertThat(modifyUserRequest.getMfaEnrollmentWindowExpiry()).isEqualTo(epoch);
        assertThat(modifyUserRequest.getMfaEnrollmentWindowExpiryInstant()).isEqualTo(date);
        assertThat(modifyUserRequest.getMfaResetAt()).isEqualTo(epoch);
        assertThat(modifyUserRequest.getMfaResetAtInstant()).isEqualTo(date);

        String output = DefaultObjectMapper.get().writeValueAsString(modifyUserRequest);
        String expected = "{\"mfa_enrollment_end\":1653082812,\"mfa_reset_at\":1653082812}";
        assertThat(output).isEqualTo(expected);

        ModifyUserRequest deserial = DefaultObjectMapper.get().readValue(expected, ModifyUserRequest.class);
        assertThat(deserial.getMfaEnrollmentWindowExpiry()).isEqualTo(epoch);
        assertThat(deserial.getMfaResetAt()).isEqualTo(epoch);
    }
}