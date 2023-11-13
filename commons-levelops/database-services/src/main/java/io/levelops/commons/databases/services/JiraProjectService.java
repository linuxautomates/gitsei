package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbJiraProjectConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraPriority;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.jira.models.JiraPriority;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraProjectService extends DatabaseService<DbJiraProject> {
    private static final String PROJECTS_TABLE = "jiraprojects";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public JiraProjectService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbJiraProject project) throws SQLException {

        String SQL = "INSERT INTO " + company + ".jiraprojects(integrationid,cloudid,name," +
                "key,leaduserid,components,isprivate) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, Integer.parseInt(project.getIntegrationId()));
            pstmt.setString(2, project.getCloudId());
            pstmt.setString(3, project.getName());
            pstmt.setString(4, project.getKey());
            pstmt.setString(5, project.getLeadUserId());
            pstmt.setArray(6,
                    conn.createArrayOf("varchar", project.getComponents().toArray()));
            pstmt.setBoolean(7, project.getIsPrivate());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        return null;
    }

    public List<String> batchUpsert(String company, List<DbJiraProject> projects) throws SQLException {
        String SQL = "INSERT INTO " + company + ".jiraprojects(integrationid,cloudid,name," +
                "key,leaduserid,components,isprivate) VALUES(?,?,?,?,?,?,?) ON CONFLICT " +
                "(integrationid,cloudid) DO UPDATE SET (name,key,leaduserid,components,isprivate) = " +
                "(EXCLUDED.name,EXCLUDED.key,EXCLUDED.leaduserid,EXCLUDED.components,EXCLUDED.isprivate) " +
                "RETURNING id";
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            int i = 0;
            for (DbJiraProject project : projects) {
                pstmt.setInt(1, Integer.parseInt(project.getIntegrationId()));
                pstmt.setString(2, project.getCloudId());
                pstmt.setString(3, project.getName());
                pstmt.setString(4, project.getKey());
                pstmt.setString(5, project.getLeadUserId());
                pstmt.setArray(6,
                        conn.createArrayOf("varchar", project.getComponents().toArray()));
                pstmt.setBoolean(7, project.getIsPrivate());
                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }

            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        batchUpsertPriorities(company, projects);
        return ids;
    }

    @NotNull
    public List<String> batchUpsertPriorities(String company, List<DbJiraProject> projects) throws SQLException {
        String SQL = "INSERT INTO " + company + ".jira_priorities (integration_id,project,scheme," +
                "priority_order,priority,description) VALUES(?,coalesce(?,'_levelops_default_'),?,?,UPPER(?),?) " +
                "ON CONFLICT " + "(integration_id,project,priority) DO UPDATE SET (scheme,priority_order,description) = " +
                "(EXCLUDED.scheme,EXCLUDED.priority_order,EXCLUDED.description) " +
                "RETURNING id";
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (DbJiraProject project : projects) {
                List<JiraPriority> jiraPriorities = project.getDefaultPriorities();
                if (jiraPriorities == null || jiraPriorities.isEmpty()) {
                    log.warn("Jira Priorities is null for integrationId {} company {}", project.getIntegrationId(), company);
                    continue;
                }
                //For now, default priorities are ingested
                String priorityScheme = "default";
                String projectName = null;
                i = upsertPriorities(ids, pstmt, i, project.getIntegrationId(), projectName, jiraPriorities, priorityScheme);
            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return ids;
    }

    private int upsertPriorities(List<String> ids, PreparedStatement pstmt, int i, String integrationId, String projectName,
                                 List<JiraPriority> jiraPriorities, String priorityScheme) throws SQLException {
        for (JiraPriority jiraPriority : jiraPriorities) {
            pstmt.setInt(1, Integer.parseInt(integrationId));
            pstmt.setString(2, projectName);//make it null
            pstmt.setString(3, priorityScheme);
            pstmt.setObject(4, jiraPriority.getPriorityOrder());
            pstmt.setString(5, StringUtils.isNotEmpty(jiraPriority.getName()) ? jiraPriority.getName() : null);
            pstmt.setString(6, StringUtils.isNotEmpty(jiraPriority.getDescription()) ? jiraPriority.getDescription() : null);
            pstmt.addBatch();
            pstmt.clearParameters();
            i++;
            if (i % 100 == 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return i;
    }

    public List<DbJiraPriority> getPriorities(String company, List<String> integId, int page, int pageSize) {
        return getPriorities(company, integId, null, page, pageSize);
    }

    public List<DbJiraPriority> getPriorities(String company, List<String> integId, String projectKey, int page, int pageSize) {
        String condition = (integId.isEmpty()) ? " " : " WHERE integration_id IN (:integration_id)  " ;
        String sql = "SELECT id,integration_id,project,scheme,priority_order,priority,description" +
                " FROM " + company + ".jira_priorities" + condition + " OFFSET :skip LIMIT :limit ";
        Map<String, Object> params = new HashMap<>();
        params.put("integration_id", integId.stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList()));
        params.put("skip", page * pageSize);
        params.put("limit", pageSize);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, JiraProjectService.prioritiesMapper());
    }

    @Override
    public Boolean update(String company, DbJiraProject integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbJiraProject> get(String company, String projectId) {
        String SQL = "SELECT id,integrationid,cloudid,name,key,leaduserid,isprivate,createdat,components" +
                " FROM " + company + ".jiraprojects WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, projectId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                List<String> components = Arrays.asList(
                        (String[]) rs.getArray("components").getArray());
                return Optional.of(DbJiraProject.builder()
                        .id(rs.getString("id"))
                        .integrationId(rs.getString("integrationid"))
                        .cloudId(rs.getString("cloudid"))
                        .name(rs.getString("name"))
                        .key(rs.getString("key"))
                        .leadUserId(rs.getString("leaduserid"))
                        .isPrivate(rs.getBoolean("isprivate"))
                        .createdAt(rs.getLong("createdat"))
                        .components(components)
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<DbJiraProject> listByFilter(String company, List<String> ids, String integrationId,
                                                      Integer pageNumber, Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(integrationId)) {
            criteria += "integrationid = ? ";
            values.add(Integer.parseInt(integrationId));
        }
        if (!CollectionUtils.isEmpty(ids)) {
            criteria += (values.size() == 0) ? "id = ANY(?) " : "AND id = ANY(?) ";
            values.add(ids.stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (values.size() == 0) {
            criteria = " ";
        }
        String SQL = "SELECT id,integrationid,cloudid,name,key,leaduserid,isprivate,createdat," +
                "components FROM " + company + ".jiraprojects" + criteria + "LIMIT "
                + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DbJiraProject> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".jiraprojects" + criteria;
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {

            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof List) {
                    Array arr = conn.createArrayOf("int", ((List) obj).toArray());
                    pstmt.setArray(i, arr);
                    pstmt2.setArray(i, arr);
                    continue;
                }
                pstmt.setObject(i, obj);
                pstmt2.setObject(i, obj);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                List<String> components = Arrays.asList(
                        (String[]) rs.getArray("components").getArray());
                retval.add(DbJiraProject.builder()
                        .id(rs.getString("id"))
                        .integrationId(rs.getString("integrationid"))
                        .cloudId(rs.getString("cloudid"))
                        .name(rs.getString("name"))
                        .key(rs.getString("key"))
                        .leadUserId(rs.getString("leaduserid"))
                        .isPrivate(rs.getBoolean("isprivate"))
                        .createdAt(rs.getLong("createdat"))
                        .components(components)
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateProject(String company, List<String> integrationIds,
                                                                          String additionalField) {
        Map<String, Object> params = new HashMap<>();
        String selectDistinctString;
        Optional<String> additionalKey;
        Map<String, List<String>> conditions = createProjectWhereClauseAndUpdateParams(params, integrationIds);
        String projectsWhere = "";
        if (conditions.get(PROJECTS_TABLE).size() > 0) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(PROJECTS_TABLE));
        }
        selectDistinctString = additionalField;
        additionalKey = Optional.of(additionalField);
        if (additionalField.equals("project_name")) {
            selectDistinctString = "name";
            additionalKey = Optional.of("name");
        }
        String SQL = "SELECT DISTINCT key," + selectDistinctString + " FROM " + company + "." +
                PROJECTS_TABLE + projectsWhere;
        List<DbAggregationResult> dbAggregationResults;

        dbAggregationResults = template.query(SQL, params, DbJiraProjectConverters.distinctRowMapper("key", additionalKey));
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    private Map<String, List<String>> createProjectWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                              List<String> integrationIds) {
        List<String> projectTableConditions = new ArrayList<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(integrationIds)) {
            projectTableConditions.add("integrationId IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        return Map.of(PROJECTS_TABLE, projectTableConditions);
    }

    @Override
    public DbListResponse<DbJiraProject> list(String company, Integer pageNumber,
                                              Integer pageSize)
            throws SQLException {
        return listByFilter(company, null, null, pageNumber, pageSize);
    }

    public Boolean deleteForIntegration(String company, String integrationId) throws SQLException {
        String SQL = "DELETE FROM " + company + ".jiraprojects WHERE integrationid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(integrationId));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".jiraprojects WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, id);
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".jiraprojects(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    cloudid VARCHAR NOT NULL,\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    integrationid INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    key VARCHAR,\n" +
                "    components VARCHAR[],\n" +
                "    isprivate BOOLEAN,\n" +
                "    leaduserid VARCHAR,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndexCreation = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_jiraprojects_compound_idx on "
                + company + ".jiraprojects (integrationid,cloudid)";

        String prioritiesSql = "CREATE TABLE IF NOT EXISTS " + company + ".jira_priorities(\n" +
                "    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,\n" +
                "    integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    project VARCHAR NOT NULL,\n" +
                "    scheme VARCHAR,\n" +
                "    priority_order INT,\n" +
                "    priority VARCHAR NOT NULL,\n" +
                "    description VARCHAR" +
                ")";

        String prioritiesSqlIndexCreation = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_jirapriorities_compound_idx on "
                + company + ".jira_priorities (integration_id,project,priority)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement projStmt = conn.prepareStatement(sql);
             PreparedStatement projIndexStmt = conn.prepareStatement(sqlIndexCreation);
             PreparedStatement priorStmt = conn.prepareStatement(prioritiesSql);
             PreparedStatement priorIndexStmt = conn.prepareStatement(prioritiesSqlIndexCreation)) {
            projStmt.execute();
            projIndexStmt.execute();
            priorStmt.execute();
            priorIndexStmt.execute();
            return true;
        }
    }

    public static RowMapper<DbJiraPriority> prioritiesMapper() {
        return (rs, rowNumber) -> DbJiraPriority.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .name(rs.getString("priority"))
                .order(rs.getInt("priority_order"))
                .scheme(rs.getString("scheme"))
                .description(rs.getString("description"))
                .build();
    }


}