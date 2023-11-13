package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbOktaConverters;
import io.levelops.commons.databases.models.database.okta.DbOktaAssociation;
import io.levelops.commons.databases.models.database.okta.DbOktaGroup;
import io.levelops.commons.databases.models.database.okta.DbOktaUser;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DatabaseService class of Okta for insert/upsert ingested data in gcs to postgres tables
 */
@Log4j2
@Service
public class OktaAggService extends DatabaseService<DbOktaUser> {

    private static final String OKTA_USERS = "okta_users";
    private static final String OKTA_GROUPS = "okta_groups";
    private static final String OKTA_ASSOCIATIONS = "okta_associations";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public OktaAggService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    /**
     * Insert Okta user into postgres table if a user with given id already exists then instead of new row the existing row is updated.
     *
     * @param company  name of the company example "foo"
     * @param oktaUser {@link DbOktaUser} that has to be inserted / upserted
     * @return inserted row id
     */
    @Override
    public String insert(String company, DbOktaUser oktaUser) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String sql = "INSERT INTO " + company + "." + OKTA_USERS + " (user_id,integration_id,status,user_type_name," +
                    "user_type_display_name,user_type_description,transitioning_to_status,login,email,first_name," +
                    "middle_name,last_name,title,display_name,nick_name,time_zone,employee_number,cost_center," +
                    "organisation,division,department,manager_id,manager,groups,updated_at) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (integration_id,user_id) " +
                    "DO UPDATE SET (status,user_type_name,user_type_display_name,user_type_description," +
                    "transitioning_to_status,login,email,first_name,middle_name,last_name,title,display_name," +
                    "nick_name,time_zone,employee_number,cost_center,organisation,division,department,manager_id," +
                    "manager,groups,updated_at) = (EXCLUDED.status,EXCLUDED.user_type_name,EXCLUDED.user_type_display_name," +
                    "EXCLUDED.user_type_description,EXCLUDED.transitioning_to_status,EXCLUDED.login,EXCLUDED.email," +
                    "EXCLUDED.first_name,EXCLUDED.middle_name,EXCLUDED.last_name,EXCLUDED.title,EXCLUDED.display_name," +
                    "EXCLUDED.nick_name,EXCLUDED.time_zone,EXCLUDED.employee_number,EXCLUDED.cost_center," +
                    "EXCLUDED.organisation,EXCLUDED.division,EXCLUDED.department,EXCLUDED.manager_id,EXCLUDED.manager," +
                    "EXCLUDED.groups,EXCLUDED.updated_at)";
            try (PreparedStatement upsertStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                upsertStatement.setString(i++, oktaUser.getUserId());
                upsertStatement.setInt(i++, NumberUtils.toInt(oktaUser.getIntegrationId()));
                upsertStatement.setString(i++, oktaUser.getStatus());
                upsertStatement.setString(i++, oktaUser.getUserTypeName());
                upsertStatement.setString(i++, oktaUser.getUserTypeDisplayName());
                upsertStatement.setString(i++, oktaUser.getUserTypeDescription());
                upsertStatement.setString(i++, oktaUser.getTransitioningToStatus());
                upsertStatement.setString(i++, oktaUser.getLogin());
                upsertStatement.setString(i++, oktaUser.getEmail());
                upsertStatement.setString(i++, oktaUser.getFirstName());
                upsertStatement.setString(i++, oktaUser.getMiddleName());
                upsertStatement.setString(i++, oktaUser.getLastName());
                upsertStatement.setString(i++, oktaUser.getTitle());
                upsertStatement.setString(i++, oktaUser.getDisplayName());
                upsertStatement.setString(i++, oktaUser.getNickName());
                upsertStatement.setString(i++, oktaUser.getTimeZone());
                upsertStatement.setString(i++, oktaUser.getEmployeeNumber());
                upsertStatement.setString(i++, oktaUser.getCostCenter());
                upsertStatement.setString(i++, oktaUser.getOrganisation());
                upsertStatement.setString(i++, oktaUser.getDivision());
                upsertStatement.setString(i++, oktaUser.getDepartment());
                upsertStatement.setString(i++, oktaUser.getManagerId());
                upsertStatement.setString(i++, oktaUser.getManager());
                upsertStatement.setObject(i++, conn.createArrayOf("varchar", oktaUser.getGroups() != null ?
                        oktaUser.getGroups().toArray() : Collections.emptyList().toArray()));
                upsertStatement.setTimestamp(i, new Timestamp(oktaUser.getLastUpdatedAt().getTime()));

                int insertedRows = upsertStatement.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to upsert row.");
                String insertedRowId = null;
                try (ResultSet rs = upsertStatement.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                return insertedRowId;
            }
        }));
    }


    @Override
    public Boolean update(String company, DbOktaUser user) {
        throw new UnsupportedOperationException();
    }

    /**
     * Insert Okta group into postgres table if a group with given id already exists then instead of new row the existing row is updated.
     *
     * @param company   name of the company example "foo"
     * @param oktaGroup {@link DbOktaGroup} that has to be inserted / upserted
     * @return inserted row id
     */
    public String insert(String company, DbOktaGroup oktaGroup) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String sql = "INSERT INTO " + company + "." + OKTA_GROUPS + " (group_id,integration_id,object_array,name," +
                    "description,type,members,updated_at) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT (group_id,integration_id) DO UPDATE SET" +
                    " (object_array,name,description,type,members,updated_at) = (EXCLUDED.object_array,EXCLUDED.name," +
                    "EXCLUDED.description,EXCLUDED.type,EXCLUDED.members,EXCLUDED.updated_at)";
            try (PreparedStatement insertStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStmt.setString(i++, oktaGroup.getGroupId());
                insertStmt.setInt(i++, NumberUtils.toInt(oktaGroup.getIntegrationId()));
                insertStmt.setObject(i++, conn.createArrayOf("varchar", oktaGroup.getObjectClass().toArray()));
                insertStmt.setString(i++, oktaGroup.getName());
                insertStmt.setString(i++, oktaGroup.getDescription());
                insertStmt.setString(i++, oktaGroup.getType());
                insertStmt.setObject(i++, conn.createArrayOf("varchar", oktaGroup.getMembers() != null ?
                        oktaGroup.getMembers().toArray() : Collections.emptyList().toArray()));
                insertStmt.setTimestamp(i, new Timestamp(oktaGroup.getLastUpdatedAt().getTime()));

                int insertedRows = insertStmt.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert row.");
                String insertedRowId = null;
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                return insertedRowId;
            }
        }));
    }

    /**
     * Insert Okta association {@link DbOktaAssociation} into postgres table if an association with given id already exists
     * then instead of new row the existing row is updated.
     *
     * @param company           name of the company example "foo"
     * @param dbOktaAssociation {@link DbOktaAssociation} to be inserted/upserted
     * @return inserted/updated row id
     */
    public String insert(String company, DbOktaAssociation dbOktaAssociation) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String sql = "INSERT INTO " + company + "." + OKTA_ASSOCIATIONS + " (integration_id,primary_id,primary_name," +
                    "primary_title,primary_description,associated_id,associated_name,associated_title," +
                    "associated_description,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT (integration_id,primary_id," +
                    "primary_name,associated_id,associated_name) DO UPDATE SET (primary_title,primary_description," +
                    "associated_title,associated_description,updated_at) = (EXCLUDED.primary_title,EXCLUDED.primary_description," +
                    "EXCLUDED.associated_title,EXCLUDED.associated_description,EXCLUDED.updated_at)";
            try (PreparedStatement insertStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStmt.setInt(i++, NumberUtils.toInt(dbOktaAssociation.getIntegrationId()));
                insertStmt.setString(i++, dbOktaAssociation.getPrimaryId());
                insertStmt.setString(i++, dbOktaAssociation.getPrimaryName());
                insertStmt.setString(i++, dbOktaAssociation.getPrimaryTitle());
                insertStmt.setString(i++, dbOktaAssociation.getPrimaryDescription());
                insertStmt.setString(i++, dbOktaAssociation.getAssociatedId());
                insertStmt.setString(i++, dbOktaAssociation.getAssociatedName());
                insertStmt.setString(i++, dbOktaAssociation.getAssociatedTitle());
                insertStmt.setString(i++, dbOktaAssociation.getAssociatedDescription());
                insertStmt.setTimestamp(i, new Timestamp(dbOktaAssociation.getLastUpdatedAt().getTime()));

                int insertedRows = insertStmt.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert row.");
                String insertedRowId = null;
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                return insertedRowId;
            }
        }));
    }

    public void cleanUpOldData(String company, Long currentTime, Long olderThanSeconds) {
        template.update("DELETE FROM " + company + "." + OKTA_GROUPS + " WHERE updated_at < :date",
                Map.of("date", new Timestamp(TimeUnit.SECONDS.toMillis(currentTime - olderThanSeconds))));
        template.update("DELETE FROM " + company + "." + OKTA_ASSOCIATIONS + " WHERE updated_at < :date",
                Map.of("date", new Timestamp(TimeUnit.SECONDS.toMillis(currentTime - olderThanSeconds))));
        template.update("DELETE FROM " + company + "." + OKTA_USERS + " WHERE updated_at < :date OR status = 'SUSPENDED'",
                Map.of("date", new Timestamp(TimeUnit.SECONDS.toMillis(currentTime - olderThanSeconds))));
    }

    @Override
    public Optional<DbOktaUser> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<DbOktaUser> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listUsers(company, pageNumber, pageSize);
    }

    public DbListResponse<DbOktaUser> listUsers(String company, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        setPagingParams(pageNumber, pageSize, params);
        String selectStatement = "SELECT * FROM " + company + "." + OKTA_USERS;
        final List<DbOktaUser> users = template.query(selectStatement, params, DbOktaConverters.userRowMapper());
        String countQuery = "SELECT count(*) from " + company + "." + OKTA_USERS;
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(users, count);
    }

    public DbListResponse<DbOktaGroup> lisGroups(String company, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        setPagingParams(pageNumber, pageSize, params);
        String selectStatement = "SELECT * FROM " + company + "." + OKTA_GROUPS;
        final List<DbOktaGroup> groups = template.query(selectStatement, params, DbOktaConverters.groupRowMapper());
        String countQuery = "SELECT count(*) from " + company + "." + OKTA_GROUPS;
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(groups, count);
    }

    public DbListResponse<DbOktaAssociation> listAssociations(String company, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        setPagingParams(pageNumber, pageSize, params);
        String selectStatement = "SELECT * FROM " + company + "." + OKTA_ASSOCIATIONS;
        final List<DbOktaAssociation> associations = template.query(selectStatement, params, DbOktaConverters.associationRowMapper());
        String countQuery = "SELECT count(*) from " + company + "." + OKTA_ASSOCIATIONS;
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(associations, count);
    }

    private void setPagingParams(Integer pageNumber, Integer pageSize, Map<String, Object> params) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> dbStatements = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + OKTA_USERS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " user_id VARCHAR NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        " status VARCHAR," +
                        " user_type_name VARCHAR NOT NULL," +
                        " user_type_display_name VARCHAR NOT NULL," +
                        " user_type_description VARCHAR," +
                        " transitioning_to_status VARCHAR," +
                        " login VARCHAR NOT NULL," +
                        " email VARCHAR NOT NULL," +
                        " first_name VARCHAR," +
                        " middle_name VARCHAR," +
                        " last_name VARCHAR," +
                        " title VARCHAR," +
                        " display_name VARCHAR," +
                        " nick_name VARCHAR," +
                        " time_zone VARCHAR," +
                        " employee_number VARCHAR," +
                        " cost_center VARCHAR," +
                        " organisation VARCHAR," +
                        " division VARCHAR," +
                        " department VARCHAR," +
                        " manager_id VARCHAR," +
                        " manager VARCHAR," +
                        " groups VARCHAR[]," +
                        " updated_at TIMESTAMP," +
                        " UNIQUE (user_id,integration_id)" +
                        " )",
                "CREATE TABLE IF NOT EXISTS " + company + "." + OKTA_GROUPS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " group_id VARCHAR NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        " object_array VARCHAR[]," +
                        " name VARCHAR NOT NULL," +
                        " description VARCHAR," +
                        " type VARCHAR NOT NULL," +
                        " members VARCHAR[]," +
                        " updated_at TIMESTAMP," +
                        " UNIQUE (group_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + OKTA_ASSOCIATIONS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        " primary_id VARCHAR NOT NULL," +
                        " primary_name VARCHAR NOT NULL," +
                        " primary_title VARCHAR," +
                        " primary_description VARCHAR," +
                        " associated_id VARCHAR NOT NULL," +
                        " associated_name VARCHAR NOT NULL," +
                        " associated_title VARCHAR," +
                        " associated_description VARCHAR," +
                        " updated_at TIMESTAMP," +
                        " UNIQUE (primary_id, primary_name, associated_id, associated_name, integration_id)" +
                        ")"
        );
        dbStatements.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    protected List<String> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                            List<String> objectClass,
                                                            List<String> types,
                                                            List<String> members,
                                                            List<String> groupIds,
                                                            List<String> integrationIds,
                                                            List<String> names) {
        List<String> groupConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(objectClass)) {
            groupConditions.add("object_array IN (:okta_objects)");
            params.put("okta_objects", objectClass);
        }
        if (CollectionUtils.isNotEmpty(types)) {
            groupConditions.add("type IN (:okta_type)");
            params.put("okta_type", types);
        }
        if (CollectionUtils.isNotEmpty(members)) {
            groupConditions.add("members IN (:okta_members)");
            params.put("members", members);
        }
        if (CollectionUtils.isNotEmpty(groupIds)) {
            groupConditions.add("group_id IN (:okta_groupIds)");
            params.put("okta_groupIds", groupIds);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            groupConditions.add("integration_id IN (:okta_integrationIds)");
            params.put("okta_integrationIds",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(names)) {
            groupConditions.add("name IN (:okta_group_names)");
            params.put("okta_group_names", names);
        }
        return groupConditions;
    }
}
