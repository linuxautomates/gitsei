package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.okta.DbOktaAssociation;
import io.levelops.commons.databases.models.database.okta.DbOktaGroup;
import io.levelops.commons.databases.models.database.okta.DbOktaUser;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;

/**
 * Converters for Db Row to Okta Objects
 */
@Log4j2
public class DbOktaConverters {

    public static RowMapper<DbOktaUser> userRowMapper() {
        return (rs, rowNumber) -> DbOktaUser.builder()
                .id(rs.getString("id"))
                .userId(rs.getString("user_id"))
                .integrationId((rs.getString("integration_id")))
                .status(rs.getString("status"))
                .userTypeName(rs.getString("user_type_name"))
                .userTypeDisplayName(rs.getString("user_type_display_name"))
                .userTypeDescription(rs.getString("user_type_description"))
                .transitioningToStatus(rs.getString("transitioning_to_status"))
                .login(rs.getString("login"))
                .email(rs.getString("email"))
                .firstName(rs.getString("first_name"))
                .middleName(rs.getString("middle_name"))
                .lastName(rs.getString("last_name"))
                .title(rs.getString("title"))
                .displayName(rs.getString("display_name"))
                .nickName(rs.getString("nick_name"))
                .timeZone(rs.getString("time_zone"))
                .employeeNumber(rs.getString("employee_number"))
                .costCenter(rs.getString("cost_center"))
                .organisation(rs.getString("organisation"))
                .division(rs.getString("division"))
                .department(rs.getString("department"))
                .managerId(rs.getString("manager_id"))
                .manager(rs.getString("manager"))
                .groups(Arrays.asList((String[]) rs.getArray("groups").getArray()))
                .lastUpdatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbOktaGroup> groupRowMapper() {
        return (rs, rowNumber) -> DbOktaGroup.builder()
                .id(rs.getString("id"))
                .groupId(rs.getString("group_id"))
                .integrationId(rs.getString("integration_id"))
                .objectClass(Arrays.asList((String[]) rs.getArray("object_array").getArray()))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .type(rs.getString("type"))
                .members(Arrays.asList((String[]) rs.getArray("members").getArray()))
                .lastUpdatedAt(rs.getTimestamp("updated_at"))
                .build();
    }

    public static RowMapper<DbOktaAssociation> associationRowMapper() {
        return (rs, rowNumber) -> DbOktaAssociation.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .primaryId(rs.getString("primary_id"))
                .primaryName(rs.getString("primary_name"))
                .primaryTitle(rs.getString("primary_title"))
                .primaryDescription(rs.getString("primary_description"))
                .associatedId(rs.getString("associated_id"))
                .associatedName(rs.getString("associated_name"))
                .associatedTitle(rs.getString("associated_title"))
                .associatedDescription(rs.getString("associated_description"))
                .lastUpdatedAt(rs.getTimestamp("updated_at"))
                .build();
    }
}
