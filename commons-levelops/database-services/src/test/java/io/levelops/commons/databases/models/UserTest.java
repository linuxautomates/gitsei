package io.levelops.commons.databases.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    @Test
    public void testDeserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/user.json");
        User output = DefaultObjectMapper.get().readValue(input, User.class);

        assertThat(output.getEmail()).isEqualTo("maxime@levelops.io");
        assertThat(output.getBcryptPassword()).isEqualTo("hunter2");
    }

    @Test
    public void serializeTest() throws JsonProcessingException {
        Instant now = Instant.now();
        User u = User.builder()
                .id("1")
                .firstName("Sid")
                .lastName("B")
                .bcryptPassword("pass")
                .userType(RoleType.ADMIN)
                .samlAuthEnabled(false)
                .passwordAuthEnabled(true)
                .mfaEnforced(true)
                .mfaEnabled(true)
                .mfaEnrollmentEndAt(now)
                .mfaResetAt(now)
                .scopes(Map.of())
                .email("sid@propelo.ai")
                .company("Propelo")
                .updatedAt(now.getEpochSecond())
                .createdAt(now.getEpochSecond())
                .build();

        ObjectMapper mapper = DefaultObjectMapper.get();
        String json = mapper.writeValueAsString(u);

        Map<String, Object> result = mapper.readValue(json, HashMap.class);

        // Test that both these fields are formatted into seconds
        assertThat(((Number) result.get("mfa_reset_at")).longValue()).isEqualTo(now.getEpochSecond());
        assertThat(((Number) result.get("mfa_enrollment_end")).longValue()).isEqualTo(now.getEpochSecond());

        u = u.toBuilder().mfaEnrollmentEndAt(null).mfaResetAt(null).build();
        json = mapper.writeValueAsString(u);

        // Ensure null values are handled correctly
        result = mapper.readValue(json, HashMap.class);
        assertThat(result.containsKey("mfa_reset_at")).isFalse();
        assertThat(result.containsKey("mfa_enrollment_end")).isFalse();
    }
}

