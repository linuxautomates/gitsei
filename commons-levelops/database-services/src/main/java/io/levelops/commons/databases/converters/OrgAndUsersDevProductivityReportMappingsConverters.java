package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class OrgAndUsersDevProductivityReportMappingsConverters {
    public static RowMapper<OrgAndUsersDevProductivityReportMappings> rowMapper() {
        return (rs, rowNumber) -> {
            List<String> orgUserIdStrings =  (rs.getArray("org_user_ids") != null && rs.getArray("org_user_ids").getArray() != null)
                    ? Arrays.asList((String[]) rs.getArray("org_user_ids").getArray()) : List.of();
            List<UUID> orgUserIds = CollectionUtils.emptyIfNull(orgUserIdStrings).stream().map(UUID::fromString).collect(Collectors.toList());

            return OrgAndUsersDevProductivityReportMappings.builder()
                    .id((UUID) rs.getObject("id"))
                    .ouID((UUID) rs.getObject("ou_id"))
                    .devProductivityProfileId(doesColumnExist("dev_productivity_profile_id",rs) ? (UUID) rs.getObject("dev_productivity_profile_id") : null)
                    .devProductivityParentProfileId(doesColumnExist("dev_productivity_parent_profile_id",rs) ? (UUID) rs.getObject("dev_productivity_parent_profile_id") : null)
                    .interval(ReportIntervalType.fromString(rs.getString("interval")))
                    .orgUserIds(orgUserIds)
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .build();
        };
    }
    public static boolean doesColumnExist(String columnName, ResultSet rs) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
