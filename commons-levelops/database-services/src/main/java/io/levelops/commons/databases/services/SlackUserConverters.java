package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.slack.SlackUser;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class SlackUserConverters {
    public static RowMapper<SlackUser> rowMapper() {
        return (rs, rowNumber) -> SlackUser.builder()
                .id((UUID) rs.getObject("id"))
                .teamId(rs.getString("team_id"))
                .userId(rs.getString("user_id"))
                .realNameNormalized(rs.getString("real_name_normalized"))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }
}
