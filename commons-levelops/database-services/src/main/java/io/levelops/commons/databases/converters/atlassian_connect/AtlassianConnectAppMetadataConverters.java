package io.levelops.commons.databases.converters.atlassian_connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectEvent;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

public class AtlassianConnectAppMetadataConverters {
    public static RowMapper<AtlassianConnectAppMetadata> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> AtlassianConnectAppMetadata.builder()
                .id(rs.getString("id"))
                .atlassianClientKey(rs.getString("atlassian_client_key"))
                .installedAppKey(rs.getString("installed_app_key"))
                .atlassianBaseUrl(rs.getString("atlassian_base_url"))
                .atlassianDisplayUrl(rs.getString("atlassian_display_url"))
                .productType(rs.getString("product_type"))
                .description(rs.getString("description"))
                .otp(rs.getString("otp"))
                .events(ParsingUtils.parseList(objectMapper, "events", AtlassianConnectEvent.class, rs.getString("events")))
                .atlassianUserAccountId(rs.getString("atlassian_user_account_id"))
                .enabled(rs.getBoolean("enabled"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }
}
