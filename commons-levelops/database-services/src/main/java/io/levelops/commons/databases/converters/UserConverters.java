package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserConverters {
    public static RowMapper<User> rowMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> resultSetMapper(mapper, rs);
    }

    public static User resultSetMapper(ObjectMapper mapper, ResultSet rs) throws SQLException {
        String id = null;
        try {
            id = rs.getString("user_id");
            List<Integer> ouRefIds = columnPresent(rs, "ou_ref_ids") ? Arrays.asList((Integer[]) rs.getArray("ou_ref_ids").getArray()) : null;
            if(ouRefIds != null && ouRefIds.contains(null))
                ouRefIds = null;
            return User.builder()
                    .id(rs.getString("user_id"))
                    .lastName(rs.getString("lastname"))
                    .firstName(rs.getString("firstname"))
                    .email(rs.getString("email"))
                    .userType(RoleType.fromString(rs.getString("usertype")))
                    .samlAuthEnabled(rs.getBoolean("samlauthenabled"))
                    .passwordAuthEnabled(rs.getBoolean("passwordauthenabled"))
                    .mfaEnabled(rs.getBoolean("mfa_enabled"))
                    .mfaEnrollmentEndAt(rs.getObject("mfa_enrollment_end") != null ? Instant.ofEpochSecond(rs.getLong("mfa_enrollment_end")) : null)
                    .mfaResetAt(rs.getObject("mfa_reset_at") != null ? Instant.ofEpochSecond(rs.getLong("mfa_reset_at")) : null)
                    .bcryptPassword(new String(rs.getBytes("bcryptpassword")))
                    .passwordResetDetails(mapper.readValue(rs.getString("passwordreset"),User.PasswordReset.class))
                    .scopes(mapper.readValue(rs.getString("scopes"),Map.class))
                    .metadata(mapper.readValue(rs.getString("metadata"), Map.class))
                    .createdAt(rs.getLong("createdat"))
                    .updatedAt(rs.getLong("updatedat"))
                    .company(columnPresent(rs, "company") ? rs.getString("company") : null)
                    .managedOURefIds(ouRefIds)
                    .build();
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize user with id=" + id);
        }
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }
}
