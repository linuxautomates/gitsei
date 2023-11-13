package io.levelops.commons.databases.services;

import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.CiCdScmMapping;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class CiCdScmMappingService extends DatabaseService<CiCdScmMapping> {
    private final int DEFAULT_PAGE_SIZE = 50;
    private final String tableName = "cicd_scm_mapping";
    private final Set<String> allowedFilters = Set.of("id", "cicd_job_run_id", "commit_id");

    private final NamedParameterJdbcTemplate template;

    public CiCdScmMappingService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class, ScmAggService.class);
    }

    @Override
    public String insert(String company, CiCdScmMapping mapping) throws SQLException {
        String insert = 
            "INSERT INTO {0}.{1}(cicd_job_run_id, commit_id, source) "
            + "VALUES(:runId::uuid, :commitId::uuid, :source) "
            + "ON CONFLICT(cicd_job_run_id,commit_id) DO NOTHING";
        
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
            MessageFormat.format(insert, company,tableName),
            new MapSqlParameterSource()
                .addValue("runId", mapping.getJobRunId().toString())
                .addValue("commitId", mapping.getCommitId().toString())
                    .addValue("source", mapping.getSource()),
            keyHolder,
            new String[]{"id"}
        );
        return count == 0 ? null : keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, CiCdScmMapping mapping) throws SQLException {
        String update = "UPDATE {0}.{1} SET {2} WHERE id = :id::uuid";
        var params = new MapSqlParameterSource()
            .addValue("id", mapping.getId().toString());
        List<String> options = Lists.newArrayList();
        if (mapping.getJobRunId() != null) {
            options.add("cicd_job_run_id = :runId::uuid");
            params.addValue("runId", mapping.getJobRunId().toString());
        }
        if (mapping.getCommitId() != null) {
            options.add("commit_id = :commitId::uuid");
            params.addValue("commitId", mapping.getCommitId().toString());
        }
        if (options.size() < 1){
            throw new SQLException("no values to update!");
        }
        options.add("updated_at = now()");
        var values = String.join(", ", options);
        var count = template.update(MessageFormat.format(update, company, tableName, values), params);
        return count > 0;
    }

    @Override
    public Optional<CiCdScmMapping> get(String company, String id) throws SQLException {
        return get(company, UUID.fromString(id));
    }

    public Optional<CiCdScmMapping> get(String company, UUID id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("id", id).build(), 0, 1);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<CiCdScmMapping> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, pageNumber, pageSize);
    }

    public DbListResponse<CiCdScmMapping> list(String company, QueryFilter filters, Integer pageNumber, Integer pageSize)
            throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        String where = "";
        if (filters.getStrictMatches() != null && !filters.getStrictMatches().isEmpty()) {
            for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
                if (allowedFilters.contains(entry.getKey()) && filters.getStrictMatches().get(entry.getKey()) != null) {
                    if (entry.getValue() instanceof UUID) {
                        conditions.add(MessageFormat.format("{0} = :{0}::uuid", entry.getKey()));
                    }
                    else {
                        conditions.add(MessageFormat.format("{0} = :{0}", entry.getKey()));
                    }
                    params.addValue(entry.getKey(), entry.getValue().toString());
                }
            }
            if (conditions.size() > 0) {
                where = "WHERE " + String.join(" AND ", conditions) + " ";
            }
        }
        String baseStatement = "FROM {0}.{1} " + where;
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber*pageSize) ;
        var records = template.query(MessageFormat.format("SELECT * " + baseStatement  + limit, company, tableName), params, (rs, row) -> {
            return CiCdScmMapping.builder()
                .id((UUID)rs.getObject("id"))
                .jobRunId((UUID) rs.getObject("cicd_job_run_id"))
                .commitId((UUID) rs.getObject("commit_id"))
                .source(rs.getString("source"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
        });
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) " + baseStatement, company, tableName), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String delete = "DELETE FROM {0}.{1} WHERE id = :id::uuid";
        var count = template.update(MessageFormat.format(delete, company, tableName), Map.of("id", id));
        return count > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1}("
            + "id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),"
            + "cicd_job_run_id    UUID NOT NULL REFERENCES {0}.cicd_job_runs(id) ON DELETE CASCADE,"
            + "commit_id          UUID NOT NULL REFERENCES {0}.scm_commits(id) ON DELETE CASCADE,"
            + "source             VARCHAR,"
            + "created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),"
            + "updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),"
            + "UNIQUE (cicd_job_run_id,commit_id)"
            + ");",

            "CREATE INDEX IF NOT EXISTS {1}_job_run_commit_idx on {0}.{1} (cicd_job_run_id,commit_id);"
        );
        ddl.stream()
            .map(statement -> MessageFormat.format(statement, company, tableName))
            .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}