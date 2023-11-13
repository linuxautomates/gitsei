package io.levelops.commons.databases.services.organization;

import io.levelops.commons.databases.models.database.organization.Workspace;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WorkspaceDatabaseService extends DatabaseService<Workspace> {

    private final NamedParameterJdbcTemplate template;

    private final List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.workspaces(\n" +
            "    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
            "    name              VARCHAR(100) NOT NULL, \n" +
            "    description       VARCHAR(300), \n" +
            "    updated_at        TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC''),\n" +
            "    created_at        TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC''),\n" +
            "    UNIQUE(name)\n" +
            ")",

            "CREATE INDEX IF NOT EXISTS workspaces_name_idx on {0}.workspaces (name)",
            "CREATE INDEX IF NOT EXISTS workspaces_updated_at_idx on {0}.workspaces (updated_at)",
            "CREATE INDEX IF NOT EXISTS workspaces_created_at_idx on {0}.workspaces (created_at)"

            // comenting out since we are reusing products instead of te new entity
            // "CREATE TABLE IF NOT EXISTS {0}.workspace_integrations (" +
            // "    workspace_id      UUID NOT NULL REFERENCES {0}.workspaces(id) ON DELETE CASCADE," +
            // "    integration_id    smallint NOT NULL REFERENCES {0}.integrations(id) ON DELETE CASCADE," +
            // "    created_at        TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC''),\n" +
            // "    CONSTRAINT worspace_integrations_pkey PRIMARY KEY (workspace_id,integration_id)" +
            // ");"
        );
    private final String INSERT_WORKSPACE_SQL_FORMAT = "INSERT INTO {0}.workspaces(name,description) VALUES(:name, :description)";
    private final String INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT = "INSERT INTO {0}.workspace_integrations(workspace_id,integration_id) VALUES(:workspaceId, :integrationId)";

    @Autowired
    public WorkspaceDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, Workspace workspace) throws SQLException {
        var id = insertForId(company, workspace);
        return id.isEmpty() ? null : id.get().toString();
    }

    public Optional<UUID> insertForId(final String company, final Workspace workspace) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                    MessageFormat.format(INSERT_WORKSPACE_SQL_FORMAT, company),
                    new MapSqlParameterSource()
                            .addValue("name",workspace.getName().trim())
                            .addValue("description", workspace.getDescription().trim()),
                    keyHolder,
                    new String[]{"id"}
            );

        if (count < 1) {
            return Optional.empty();
        }
        var id = (UUID)keyHolder.getKeys().get("id");
        if (CollectionUtils.isEmpty(workspace.getIntegrationIds())) {
            return Optional.of(id);
        }
        // Insert integration mappings
        var values = new ArrayList<Map<String, Object>>();
        workspace.getIntegrationIds().forEach(integrationId -> {
            values.add(Map.of(
                "workspaceId", id,
                "integrationId", integrationId)
            );
        });
        int[] count2 = this.template.batchUpdate(
            MessageFormat.format(INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT, company),
            values.toArray(new Map[0])
        );
        if (Arrays.stream(count2).anyMatch(r -> r < 1)) {
            log.warn("[{}] Unable to insert all workspace - integrations.. {} -> {}", company, id, workspace.getIntegrationIds());
        }
        return Optional.of(id);
    }

    @Override
    public Boolean update(String company, Workspace workspace) throws SQLException {
        String SQL = "UPDATE {0}.workspaces SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        //empty string is NOT a valid name so we skip it
        if (StringUtils.isNotEmpty(workspace.getName())) {
            updates = "name = ?";
            values.add(workspace.getName().trim());
        }
        //empty string is valid description
        if (workspace.getDescription() != null) {
            updates = StringUtils.isEmpty(updates) ? "description = ?" : updates + ", description = ?";
            values.add(workspace.getDescription());
        }

        //nothing to update.
        if (values.size() == 0 && CollectionUtils.isEmpty(workspace.getIntegrationIds())) {
            return false;
        }

        if (values.size() > 0) {
            updates += StringUtils.isEmpty(updates) ? "updated_at = ?" : ", updated_at = ?";
            values.add(Instant.now());

            SQL = SQL + updates + condition;

            try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(SQL,
                        Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 1; i <= values.size(); i++) {
                    pstmt.setObject(i, values.get(i - 1));
                }
                pstmt.setObject(values.size() + 1, workspace.getId());
                pstmt.executeUpdate();
            }
        }
        if (CollectionUtils.isNotEmpty(workspace.getIntegrationIds())) {
            // remove previous integrations
            var result = template.update(MessageFormat.format("DELETE FROM {0}.workspaces WHERE workspace_id = :workspaceId", company), Map.of("workspaceId", workspace.getId()));
            log.debug("[{}] deleted '{}' previous integrations.", company, result);
            // insert new integrations
            int[] count2 = this.template.batchUpdate(
                MessageFormat.format(INSERT_WORKSPACE_INTEGRATION_SQL_FORMAT + " ON CONFLICT(workspace_id, integration_id) DO NOTHING", company),
                values.toArray(new Map[0])
            );
            if (Arrays.stream(count2).anyMatch(r -> r < 1)) {
                log.warn("[{}] Unable to insert all workspace - integrations.. {} -> {}", company, workspace.getId(), workspace.getIntegrationIds());
            }
        }
        return false;
    }

    @Override
    public Optional<Workspace> get(String company, final String workspaceId) {
        return get(company, UUID.fromString(workspaceId));
    }

    public Optional<Workspace> get(String company, final UUID workspaceId) {
        String SQL = "SELECT id, name, description, updated_at, created_at, (SELECT array_agg(integration_id::int) FROM {0}.workspace_integrations WHERE workspace_id = id) integration_ids "
                + " FROM {0}.workspaces WHERE id = :workspaceId ";
        return Optional.of(template.queryForObject(MessageFormat.format(SQL, company), Map.of("workspaceId", workspaceId), (rs,i) -> {
                return Workspace.builder()
                        .id((UUID)rs.getObject("id"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .updatedAt(rs.getTimestamp("updated_at").toInstant())
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .integrationIds(Arrays.asList((Integer[])rs.getArray("integration_ids").getArray()).stream()
                            .map(o -> (Integer)o).collect(Collectors.toSet()))
                        .build();
            }));
    }

    public DbListResponse<Workspace> listByFilter(String company, String name, Set<UUID> workspacesIds, Set<Integer> integrationIds,
                                                Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, name, workspacesIds, integrationIds, null, null, pageNumber, pageSize);
    }

    public DbListResponse<Workspace> listByFilter(String company, String name, Set<UUID> workspacesIds, Set<Integer> integrationIds,
                                                Instant updatedAtStart, Instant updatedAtEnd,
                                                Integer pageNumber, Integer pageSize) throws SQLException {
        String criteria = " WHERE ";
        MapSqlParameterSource values = new MapSqlParameterSource();
        if (StringUtils.isNotEmpty(name)) {
            criteria += "name ILIKE :name ";
            values.addValue("name", "%" + name + "%");
        }
        if (CollectionUtils.isNotEmpty(workspacesIds)) {
            criteria += (values.getValues().size() == 0) ?
                    "id = ANY(?::int[]) " : "AND id = ANY(?::int[]) ";
            values.addValue("workspacesIds", workspacesIds);
        }
        if (updatedAtStart != null) {
            criteria += (values.getValues().size() == 0 ? "" : "AND ") + "updated_at > :updatedAtStart ";
            values.addValue("updatedAtStart", updatedAtStart);
        }
        if (updatedAtEnd != null) {
            criteria += (values.getValues().size() == 0 ? "" : "AND ") + "updated_at < :updatedAtEnd ";
            values.addValue("updatedAtEnd", updatedAtEnd);
        }
        if (values.getValues().size() == 0) {
            criteria = "";
        }
        String SQL = "SELECT id,name,description,updated_at,created_at,(SELECT array_agg(integration_id::int) FROM {0}.workspace_integrations WHERE workspace_id = id) integration_ids"
                + " FROM " + company + ".workspaces " + criteria + "ORDER BY updated_at DESC LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        List<Workspace> retval = template.query(MessageFormat.format(SQL, company), values, (rs, i) -> {
            return Workspace.builder()
                    .id((UUID)rs.getObject("id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .integrationIds(Arrays.asList((Integer[])rs.getArray("integration_ids").getArray()).stream()
                        .map(o -> (Integer)o).collect(Collectors.toSet()))
                    .build();
        });
        String countSQL = "SELECT COUNT(*) FROM ( SELECT id,name FROM " + company + ".workspaces " + criteria + ") AS d";
        Integer totCount = template.queryForObject(countSQL, values, Integer.class);
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<Workspace> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return delete(company, UUID.fromString(id));
    }

    public Boolean delete(final String company, final UUID id) throws SQLException {
        return template.update("DELETE FROM " + company + ".workspaces WHERE id = :id", Map.of("id", id)) > 0;
    }


    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(statement -> template.getJdbcTemplate()
            .execute(MessageFormat.format(statement, company)));
        return true;
    }
}