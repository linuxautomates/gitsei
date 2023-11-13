package io.levelops.commons.databases.services.scm;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.OrgAndUsersDevProductivityReportMappingsConverters;
import io.levelops.commons.databases.converters.VelocityConfigConverters;
import io.levelops.commons.databases.converters.scm.DbScmCommitPRMappingConverters;
import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.scm.DbScmCommitPRMapping;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ScmCommitPullRequestMappingDBService extends DatabaseService<DbScmCommitPRMapping> {
    private final static Integer BATCH_SIZE = 100;
    private final static String INSERT_MAPPING_SQL_FORMAT = "INSERT INTO %s.scm_commit_pullrequest_mappings(scm_commit_id, scm_pullrequest_id) VALUES(:scm_commit_id, :scm_pullrequest_id) ON CONFLICT(scm_commit_id, scm_pullrequest_id) DO NOTHING";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public ScmCommitPullRequestMappingDBService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ScmAggService.class);
    }
    // endregion

    // region insert & batch inserts
    @Override
    public String insert(String company, DbScmCommitPRMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    private SqlParameterSource constructMappingParameterSource(DbScmCommitPRMapping scmCommitPRMapping) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scm_commit_id", scmCommitPRMapping.getScmCommitId());
        params.addValue("scm_pullrequest_id", scmCommitPRMapping.getScmPullrequestId());
        return params;
    }
    public void batchInsert(String company, List<DbScmCommitPRMapping> mappings) throws SQLException {
        if(CollectionUtils.isEmpty(mappings)) {
            return;
        }
        String insertSql = String.format(INSERT_MAPPING_SQL_FORMAT, company);
        for (List<DbScmCommitPRMapping> currentBatch : ListUtils.partition(mappings, BATCH_SIZE)) {
            List<SqlParameterSource> parameterSources = currentBatch.stream().map(m -> constructMappingParameterSource(m)).collect(Collectors.toList());
            int[] updateCounts = template.batchUpdate(insertSql, parameterSources.toArray(new SqlParameterSource[0]));
        }
        return;
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, DbScmCommitPRMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region get and list commons
    public DbListResponse<DbScmCommitPRMapping> getBatch(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                       final List<UUID> scmCommitIds, final List<UUID> scmPullrequestIds) {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(scmCommitIds)) {
            criterias.add("scm_commit_id in (:scm_commit_ids)");
            params.put("scm_commit_ids", scmCommitIds);
        }
        if(CollectionUtils.isNotEmpty(scmPullrequestIds)) {
            criterias.add("scm_pullrequest_id in (:scm_pullrequest_ids)");
            params.put("scm_pullrequest_ids", scmPullrequestIds);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".scm_commit_pullrequest_mappings";
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY created_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmCommitPRMapping> results = template.query(selectSql, params, DbScmCommitPRMappingConverters.rowMapper());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                log.info("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    // endregion


    // region get and list commons
    @Override
    public Optional<DbScmCommitPRMapping> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)), null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    // region get and list commons
    @Override
    public DbListResponse<DbScmCommitPRMapping> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null);
    }
    public DbListResponse<DbScmCommitPRMapping> listByFilter(String company, Integer pageNumber, Integer pageSize, final List<UUID> ids,
                                                             final List<UUID> scmCommitIds, final List<UUID> scmPullrequestIds) {
        return getBatch(company, pageNumber, pageSize, ids, scmCommitIds, scmPullrequestIds);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlStatements = new ArrayList<>();
        String sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".scm_commit_pullrequest_mappings(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    scm_commit_id UUID NOT NULL REFERENCES " + company + ".scm_commits(id) ON DELETE CASCADE,\n" +
                "    scm_pullrequest_id UUID NOT NULL REFERENCES " + company + ".scm_pullrequests(id) ON DELETE CASCADE,\n" +
                "    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()\n" +
                ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_scm_commit_pullrequest_mappings_scm_commit_id_scm_pullrequest_id_idx on " + company + ".scm_commit_pullrequest_mappings (scm_commit_id,scm_pullrequest_id)";
        sqlStatements.add(sqlStatement);

        sqlStatements.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
