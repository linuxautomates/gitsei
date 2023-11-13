package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbCoverityConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.coverity.DbCoverityDefect;
import io.levelops.commons.databases.models.database.coverity.DbCoveritySnapshot;
import io.levelops.commons.databases.models.database.coverity.DbCoverityStream;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CoverityDefectFilter;
import io.levelops.commons.databases.models.filters.CoveritySnapshotFilter;
import io.levelops.commons.databases.models.filters.CoverityStreamFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.coverity.models.CoverityAttributes;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CoverityDatabaseService extends DatabaseService<DbCoverityDefect> {

    private static final String STREAMS_TABLE = "coverity_streams";
    private static final String SNAPSHOTS_TABLE = "coverity_snapshots";
    private static final String DEFECTS_TABLE = "coverity_defects";
    private static final Set<String> DEFECT_SORTABLE_COLUMNS = Set.of("cid", "checker_name", "component_name", "cwe", "category", "impact", "kind",
            "type", "domain", "first_detected_at", "first_detected_by", "first_detected_stream", "first_detected_snapshot_id", "last_detected_at",
            "last_detected_stream", "last_detected_snapshot_id", "occurrence_count");
    private static final Set<String> SNAPSHOT_SORTABLE_COLUMNS = Set.of("snapshot_id", "analysis_host", "analysis_version", "time_taken", "build_failure_count",
            "build_success_count", "commit_user", "snapshot_created_at");
    private static final Set<String> STREAM_SORTABLE_COLUMNS = Set.of("name", "language", "project", "triage_store_id");

    private final NamedParameterJdbcTemplate template;

    private final Set<CoverityDefectFilter.DISTINCT> defectsStackSupported = Set.of(
            CoverityDefectFilter.DISTINCT.type,
            CoverityDefectFilter.DISTINCT.checker_name,
            CoverityDefectFilter.DISTINCT.component_name,
            CoverityDefectFilter.DISTINCT.category,
            CoverityDefectFilter.DISTINCT.first_detected,
            CoverityDefectFilter.DISTINCT.first_detected_stream,
            CoverityDefectFilter.DISTINCT.impact,
            CoverityDefectFilter.DISTINCT.last_detected,
            CoverityDefectFilter.DISTINCT.last_detected_stream,
            CoverityDefectFilter.DISTINCT.domain,
            CoverityDefectFilter.DISTINCT.kind,
            CoverityDefectFilter.DISTINCT.snapshot_created,
            CoverityDefectFilter.DISTINCT.file,
            CoverityDefectFilter.DISTINCT.function);

    private final Set<CoveritySnapshotFilter.DISTINCT> snapshotsStackSupported = Set.of(
            CoveritySnapshotFilter.DISTINCT.snapshot_id,
            CoveritySnapshotFilter.DISTINCT.analysis_host,
            CoveritySnapshotFilter.DISTINCT.commit_user,
            CoveritySnapshotFilter.DISTINCT.analysis_version);

    @Autowired
    protected CoverityDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    public List<String> upsertStream(String company, List<DbCoverityStream> dbCoverityStreams) throws SQLException {
        String sql = "INSERT INTO " + company + "." + STREAMS_TABLE +
                " (name,language,integration_id,project,triage_store_id) VALUES (?,?,?,?,?) ON CONFLICT (name,project,integration_id) DO NOTHING";
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insertStream = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (DbCoverityStream dbCoverityStream : dbCoverityStreams) {
                insertStream.setString(1, dbCoverityStream.getName());
                insertStream.setString(2, dbCoverityStream.getLanguage());
                insertStream.setInt(3, dbCoverityStream.getIntegrationId());
                insertStream.setString(4, dbCoverityStream.getProject());
                insertStream.setString(5, dbCoverityStream.getTriageStoreId());
                insertStream.addBatch();
                insertStream.clearParameters();
                insertStream.executeBatch();
                ResultSet rs = insertStream.getGeneratedKeys();
                while (rs.next()) {
                    String idInserted = rs.getString("id");
                    ids.add(idInserted);
                }
            }
        }
        return ids;
    }

    public List<String> upsertSnapshot(String company, List<DbCoveritySnapshot> dbCoveritySnapshots) throws SQLException {
        String sql = "INSERT INTO " + company + "." + SNAPSHOTS_TABLE +
                " (stream_id,snapshot_id,analysis_host,analysis_version,integration_id,time_taken,build_failure_count,build_success_count,commit_user,snapshot_created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,date_trunc('second',? ::TIMESTAMP)) ON CONFLICT (snapshot_id,integration_id) DO NOTHING";
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insertSnapshot = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (DbCoveritySnapshot dbCoveritySnapshot : dbCoveritySnapshots) {
                insertSnapshot.setObject(1, UUID.fromString(dbCoveritySnapshot.getStreamId()));
                insertSnapshot.setInt(2, dbCoveritySnapshot.getSnapshotId());
                insertSnapshot.setString(3, dbCoveritySnapshot.getAnalysisHost());
                insertSnapshot.setString(4, dbCoveritySnapshot.getAnalysisVersion());
                insertSnapshot.setInt(5, dbCoveritySnapshot.getIntegrationId());
                insertSnapshot.setInt(6, dbCoveritySnapshot.getTimeTaken());
                insertSnapshot.setInt(7, dbCoveritySnapshot.getBuildFailureCount());
                insertSnapshot.setInt(8, dbCoveritySnapshot.getBuildSuccessCount());
                insertSnapshot.setString(9, dbCoveritySnapshot.getCommitUser());
                insertSnapshot.setTimestamp(10, dbCoveritySnapshot.getSnapshotCreatedAt());
                insertSnapshot.addBatch();
                insertSnapshot.clearParameters();
                insertSnapshot.executeBatch();
                ResultSet rs = insertSnapshot.getGeneratedKeys();
                while (rs.next()) {
                    String idInserted = rs.getString("id");
                    ids.add(idInserted);
                }
            }
        }
        return ids;
    }

    @Override
    public String insert(String company, DbCoverityDefect defect) throws SQLException {
        UUID id = null;
        String sql = "INSERT INTO " + company + "." + DEFECTS_TABLE +
                " (snapshot_id,cid,checker_name,integration_id,component_name,cwe,attributes,category,impact,kind,type,domain,file_path,function_name,first_detected_at," +
                "first_detected_by,first_detected_stream,first_detected_snapshot_id,last_detected_at,last_detected_stream,last_detected_snapshot_id,merge_key,misra_category,occurrence_count) " +
                "VALUES (?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?,date_trunc('second',? ::TIMESTAMP),?,?,?,date_trunc('second',? ::TIMESTAMP),?,?,?,?,?) ON CONFLICT (cid,snapshot_id,integration_id) DO NOTHING";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            pstmt.setObject(i++, UUID.fromString(defect.getSnapshotId()));
            pstmt.setObject(i++, defect.getCid());
            pstmt.setObject(i++, defect.getCheckerName());
            pstmt.setObject(i++, defect.getIntegrationId());
            pstmt.setObject(i++, defect.getComponentName());
            pstmt.setObject(i++, defect.getCwe());
            pstmt.setObject(i++, DefaultObjectMapper.get()
                    .writeValueAsString(simplifyAttributes(defect.getAttributes())));
            pstmt.setObject(i++, defect.getCategory());
            pstmt.setObject(i++, defect.getImpact());
            pstmt.setObject(i++, defect.getKind());
            pstmt.setObject(i++, defect.getType());
            pstmt.setObject(i++, defect.getDomain());
            pstmt.setObject(i++, defect.getFilePath());
            pstmt.setObject(i++, defect.getFunctionName());
            pstmt.setTimestamp(i++, defect.getFirstDetectedAt());
            pstmt.setObject(i++, defect.getFirstDetectedBy());
            pstmt.setObject(i++, defect.getFirstDetectedStream());
            pstmt.setObject(i++, defect.getFirstDetectedSnapshotId());
            pstmt.setTimestamp(i++, defect.getLastDetectedAt());
            pstmt.setObject(i++, defect.getLastDetectedStream());
            pstmt.setObject(i++, defect.getLastDetectedSnapshotId());
            pstmt.setObject(i++, defect.getMergeKey());
            pstmt.setObject(i++, defect.getMisraCategory());
            pstmt.setObject(i, defect.getOccurrenceCount());

            pstmt.executeUpdate();

            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    id = (UUID) rs.getObject("id");
                }
                return id == null ? "" : id.toString();
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to insert dashboard.", e);
        }
    }

    public Optional<DbCoverityStream> getStream(String company, String name, String project, String integrationId) {
        Validate.notBlank(name, "Missing stream name.");
        Validate.notBlank(project, "Missing project.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + STREAMS_TABLE
                + " WHERE name = :name AND project = :project AND integration_id = :integid";
        Map<String, Object> params = Map.of("name", name, "project", project, "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbCoverityStream> data = template.query(sql, params, DbCoverityConverters.streamRowMapper());
        return data.stream().findFirst();
    }

    public Optional<DbCoveritySnapshot> getSnapshot(String company, String snapshotId, String integrationId) {
        Validate.notBlank(snapshotId, "Missing snapshotId.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + SNAPSHOTS_TABLE
                + " WHERE snapshot_id = :snapshotId AND integration_id = :integid";
        Map<String, Object> params = Map.of("snapshotId", NumberUtils.toInt(snapshotId), "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbCoveritySnapshot> data = template.query(sql, params, DbCoverityConverters.snapshotRowMapper());
        return data.stream().findFirst();
    }

    public Optional<DbCoverityDefect> getDefect(String company, String integrationId, String cid) {
        Validate.notBlank(cid, "Missing cid.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + DEFECTS_TABLE
                + " WHERE integration_id = :integid AND cid = :cid";
        Map<String, Object> params = Map.of("integid", NumberUtils.toInt(integrationId), "cid", NumberUtils.toInt(cid));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbCoverityDefect> data = template.query(sql, params, DbCoverityConverters.defectRowMapper());
        return data.stream().findFirst();
    }

    public Map<String, List<String>> createDefectWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                            CoverityDefectFilter filter,
                                                                            String prefix) {
        List<String> defectTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getCids())) {
            defectTableConditions.add(prefix + "cid IN (:cids)");
            params.put("cids", filter.getCids().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCategories())) {
            defectTableConditions.add(prefix + "category IN (:categories)");
            params.put("categories", filter.getCategories());
        }
        if (CollectionUtils.isNotEmpty(filter.getCheckerNames())) {
            defectTableConditions.add(prefix + "checker_name IN (:checker_names)");
            params.put("checker_names", filter.getCheckerNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getSnapshotIds())) {
            defectTableConditions.add(prefix + "snapshot_id IN (:snapshot_ids)");
            params.put("snapshot_ids", filter.getSnapshotIds().stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getComponentNames())) {
            defectTableConditions.add(prefix + "component_name IN (:component_names)");
            params.put("component_names", filter.getComponentNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            defectTableConditions.add(prefix + "integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getImpacts())) {
            defectTableConditions.add(prefix + "impact IN (:impacts)");
            params.put("impacts", filter.getImpacts());
        }
        if (CollectionUtils.isNotEmpty(filter.getKinds())) {
            defectTableConditions.add(prefix + "kind IN (:kinds)");
            params.put("kinds", filter.getKinds());
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            defectTableConditions.add(prefix + "type IN (:types)");
            params.put("types", filter.getTypes());
        }
        if (CollectionUtils.isNotEmpty(filter.getDomains())) {
            defectTableConditions.add(prefix + "domain IN (:domains)");
            params.put("domains", filter.getDomains());
        }
        if (CollectionUtils.isNotEmpty(filter.getFilePaths())) {
            defectTableConditions.add(prefix + "file_path IN (:file_paths)");
            params.put("file_paths", filter.getFilePaths());
        }
        if (CollectionUtils.isNotEmpty(filter.getFunctionNames())) {
            defectTableConditions.add(prefix + "function_name IN (:function_names)");
            params.put("function_names", filter.getFunctionNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getFirstDetectedStreams())) {
            defectTableConditions.add(prefix + "first_detected_stream IN (:first_detected_streams)");
            params.put("first_detected_streams", filter.getFirstDetectedStreams());
        }
        if (CollectionUtils.isNotEmpty(filter.getFirstDetectedSnapshotIds())) {
            defectTableConditions.add(prefix + "first_detected_snapshot_id IN (:first_detected_snapshot_ids)");
            params.put("first_detected_snapshot_ids",
                    filter.getFirstDetectedSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getLastDetectedStreams())) {
            defectTableConditions.add(prefix + "last_detected_stream IN (:last_detected_streams)");
            params.put("last_detected_streams", filter.getLastDetectedStreams());
        }
        if (CollectionUtils.isNotEmpty(filter.getLastDetectedSnapshotIds())) {
            defectTableConditions.add(prefix + "last_detected_snapshot_id IN (:last_detected_snapshot_ids)");
            params.put("last_detected_snapshot_ids",
                    filter.getLastDetectedSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCids())) {
            defectTableConditions.add(prefix + "cid NOT IN (:exclude_cids)");
            params.put("exclude_cids", filter.getExcludeCids().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCategories())) {
            defectTableConditions.add(prefix + "category NOT IN (:exclude_categories)");
            params.put("exclude_categories", filter.getExcludeCategories());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCheckerNames())) {
            defectTableConditions.add(prefix + "checker_name NOT IN (:exclude_checker_names)");
            params.put("exclude_checker_names", filter.getExcludeCheckerNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeComponentNames())) {
            defectTableConditions.add(prefix + "component_name NOT IN (:exclude_component_names)");
            params.put("exclude_component_names", filter.getExcludeComponentNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeImpacts())) {
            defectTableConditions.add(prefix + "impact NOT IN (:exclude_impacts)");
            params.put("exclude_impacts", filter.getExcludeImpacts());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeKinds())) {
            defectTableConditions.add(prefix + "kind NOT IN (:exclude_kinds)");
            params.put("exclude_kinds", filter.getExcludeKinds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            defectTableConditions.add(prefix + "type NOT IN (:exclude_types)");
            params.put("exclude_types", filter.getExcludeTypes());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeDomains())) {
            defectTableConditions.add(prefix + "domain NOT IN (:exclude_domains)");
            params.put("exclude_domains", filter.getExcludeDomains());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeFilePaths())) {
            defectTableConditions.add(prefix + "file_path NOT IN (:exclude_file_paths)");
            params.put("exclude_file_paths", filter.getExcludeFilePaths());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeFunctionNames())) {
            defectTableConditions.add(prefix + "function_name NOT IN (:exclude_function_names)");
            params.put("exclude_function_names", filter.getExcludeFunctionNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeFirstDetectedStreams())) {
            defectTableConditions.add(prefix + "first_detected_stream NOT IN (:exclude_first_detected_streams)");
            params.put("exclude_first_detected_streams", filter.getExcludeFirstDetectedStreams());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeFirstDetectedSnapshotIds())) {
            defectTableConditions.add(prefix + "first_detected_snapshot_id NOT IN (:exclude_first_detected_snapshot_ids)");
            params.put("exclude_first_detected_snapshot_ids", filter.getExcludeFirstDetectedSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLastDetectedStreams())) {
            defectTableConditions.add(prefix + "last_detected_stream NOT IN (:exclude_last_detected_streams)");
            params.put("exclude_last_detected_streams", filter.getExcludeLastDetectedStreams());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLastDetectedSnapshotIds())) {
            defectTableConditions.add(prefix + "last_detected_snapshot_id NOT IN (:exclude_last_detected_snapshot_ids)");
            params.put("exclude_last_detected_snapshot_ids", filter.getExcludeLastDetectedSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (filter.getOccurrenceCount() != null) {
            if (filter.getOccurrenceCount().getLeft() != null) {
                defectTableConditions.add(prefix + "occurrence_count > (" + filter.getOccurrenceCount().getLeft() + ")");
            }
            if (filter.getOccurrenceCount().getRight() != null) {
                defectTableConditions.add(prefix + "occurrence_count < (" + filter.getOccurrenceCount().getRight() + ")");
            }
        }
        if (filter.getFirstDetectedAt() != null) {
            if (filter.getFirstDetectedAt().getLeft() != null) {
                defectTableConditions.add(prefix + "first_detected_at > TO_TIMESTAMP(" + filter.getFirstDetectedAt().getLeft() + ")");
            }
            if (filter.getFirstDetectedAt().getRight() != null) {
                defectTableConditions.add(prefix + "first_detected_at < TO_TIMESTAMP(" + filter.getFirstDetectedAt().getRight() + ")");
            }
        }
        if (filter.getLastDetectedAt() != null) {
            if (filter.getLastDetectedAt().getLeft() != null) {
                defectTableConditions.add(prefix + "last_detected_at > TO_TIMESTAMP(" + filter.getLastDetectedAt().getLeft() + ")");
            }
            if (filter.getLastDetectedAt().getRight() != null) {
                defectTableConditions.add(prefix + "last_detected_at < TO_TIMESTAMP(" + filter.getLastDetectedAt().getRight() + ")");
            }
        }
        if (filter.getSnapshotCreatedRange() != null) {
            if (filter.getSnapshotCreatedRange().getLeft() != null) {
                defectTableConditions.add("snapshots.snapshot_created_at > TO_TIMESTAMP(" + filter.getSnapshotCreatedRange().getLeft() + ")");
            }
            if (filter.getSnapshotCreatedRange().getRight() != null) {
                defectTableConditions.add("snapshots.snapshot_created_at < TO_TIMESTAMP(" + filter.getSnapshotCreatedRange().getRight() + ")");
            }
        }
        return Map.of(DEFECTS_TABLE, defectTableConditions);
    }

    public Map<String, List<String>> createSnapshotWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                              CoveritySnapshotFilter filter) {
        List<String> snapshotTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getStreamIds())) {
            snapshotTableConditions.add("stream_id IN (:stream_ids)");
            params.put("stream_ids", filter.getStreamIds().stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getSnapshotIds())) {
            snapshotTableConditions.add("snapshot_id IN (:snapshot_ids)");
            params.put("snapshot_ids", filter.getSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAnalysisHosts())) {
            snapshotTableConditions.add("analysis_host IN (:analysis_hosts)");
            params.put("analysis_hosts", filter.getAnalysisHosts());
        }
        if (CollectionUtils.isNotEmpty(filter.getAnalysisVersions())) {
            snapshotTableConditions.add("analysis_version IN (:analysis_versions)");
            params.put("analysis_versions", filter.getAnalysisVersions());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            snapshotTableConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCommitUsers())) {
            snapshotTableConditions.add("commit_user IN (:commit_users)");
            params.put("commit_users", filter.getCommitUsers());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeSnapshotIds())) {
            snapshotTableConditions.add("snapshot_id NOT IN (:exclude_snapshot_ids)");
            params.put("exclude_snapshot_ids", filter.getExcludeSnapshotIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAnalysisHosts())) {
            snapshotTableConditions.add("analysis_host NOT IN (:exclude_analysis_hosts)");
            params.put("exclude_analysis_hosts", filter.getExcludeAnalysisHosts());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAnalysisVersions())) {
            snapshotTableConditions.add("analysis_version NOT IN (:exclude_analysis_versions)");
            params.put("exclude_analysis_versions", filter.getExcludeAnalysisVersions());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommitUsers())) {
            snapshotTableConditions.add("commit_user NOT IN (:exclude_commit_users)");
            params.put("exclude_commit_users", filter.getExcludeCommitUsers());
        }
        if (filter.getBuildFailureCount() != null) {
            if (filter.getBuildFailureCount().getLeft() != null) {
                snapshotTableConditions.add("build_failure_count > (" + filter.getBuildFailureCount().getLeft() + ")");
            }
            if (filter.getBuildFailureCount().getRight() != null) {
                snapshotTableConditions.add("build_failure_count < (" + filter.getBuildFailureCount().getRight() + ")");
            }
        }
        if (filter.getBuildSuccessCount() != null) {
            if (filter.getBuildSuccessCount().getLeft() != null) {
                snapshotTableConditions.add("build_success_count > (" + filter.getBuildSuccessCount().getLeft() + ")");
            }
            if (filter.getBuildSuccessCount().getRight() != null) {
                snapshotTableConditions.add("build_success_count < (" + filter.getBuildSuccessCount().getRight() + ")");
            }
        }
        if (filter.getSnapshotCreatedAt() != null) {
            if (filter.getSnapshotCreatedAt().getLeft() != null) {
                snapshotTableConditions.add("snapshot_created_at > TO_TIMESTAMP(" + filter.getSnapshotCreatedAt().getLeft() + ")");
            }
            if (filter.getSnapshotCreatedAt().getRight() != null) {
                snapshotTableConditions.add("snapshot_created_at < TO_TIMESTAMP(" + filter.getSnapshotCreatedAt().getRight() + ")");
            }
        }
        return Map.of(SNAPSHOTS_TABLE, snapshotTableConditions);
    }

    public Map<String, List<String>> createStreamWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                            CoverityStreamFilter filter) {
        List<String> streamTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getNames())) {
            streamTableConditions.add("name IN (:names)");
            params.put("names", filter.getNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getLanguages())) {
            streamTableConditions.add("language IN (:languages)");
            params.put("languages", filter.getLanguages());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            streamTableConditions.add("project IN (:projects)");
            params.put("projects", filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getTriageStoreIds())) {
            streamTableConditions.add("triage_store_id IN (:triage_store_ids)");
            params.put("triage_store_ids", filter.getTriageStoreIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            streamTableConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeNames())) {
            streamTableConditions.add("name NOT IN (:exclude_names)");
            params.put("exclude_names", filter.getExcludeNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLanguages())) {
            streamTableConditions.add("language NOT IN (:exclude_languages)");
            params.put("exclude_languages", filter.getExcludeLanguages());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            streamTableConditions.add("project NOT IN (:exclude_projects)");
            params.put("exclude_projects", filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTriageStoreIds())) {
            streamTableConditions.add("triage_store_id NOT IN (:exclude_triage_store_ids)");
            params.put("exclude_triage_store_ids", filter.getExcludeTriageStoreIds());
        }
        return Map.of(STREAMS_TABLE, streamTableConditions);
    }

    @Override
    public Boolean update(String company, DbCoverityDefect t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbCoverityDefect> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbCoverityDefect> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    public DbListResponse<DbCoverityDefect> list(String company,
                                                 CoverityDefectFilter filter,
                                                 Map<String, SortingOrder> sortBy,
                                                 Integer pageNumber,
                                                 Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String defectsWhere = "";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sortByKey = sortBy.entrySet()
                .stream()
                .findFirst()
                .map(entry -> {
                    if (DEFECT_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "cid";
                })
                .orElse("cid");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.ASC);

        Map<String, List<String>> conditions = createDefectWhereClauseAndUpdateParams(params, filter, "defects.");
        if (conditions.get(DEFECTS_TABLE).size() > 0)
            defectsWhere = " AND " + String.join(" AND ", conditions.get(DEFECTS_TABLE));

        String snapshotQueryWhere = "";
        boolean needTrend = false;
        if(filter.getSnapshotCreatedRange() != null)
            needTrend = (filter.getSnapshotCreatedRange().getLeft() != null || filter.getSnapshotCreatedRange().getRight() != null);
        Map<String, String> snapshotQuery = getSnapshotQuery(company, params, filter.getSnapshotCreatedAt(), needTrend);
        snapshotQueryWhere = " WHERE " + String.join(" AND ", snapshotQuery.get("snapshot_query"));

        String baseWithClause = "WITH defects AS ";
        String baseWithSql = baseWithClause
                + "( SELECT * FROM "
                + company + "." + DEFECTS_TABLE + " AS defects, " + company + "." + SNAPSHOTS_TABLE + " AS snapshots," + company + "." + STREAMS_TABLE + " AS streams "
                + snapshotQueryWhere
                + defectsWhere
                + " )";

        List<DbCoverityDefect> results = List.of();
        String sql = "";
        String pagination = " OFFSET :skip LIMIT :limit";
        if (pageSize > 0) {
            sql = baseWithSql + "SELECT *"
                    + " FROM defects  ORDER BY " + sortByKey + " " + sortOrder;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql + pagination, params, DbCoverityConverters.defectRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbCoveritySnapshot> listSnapshots(String company,
                                                            CoveritySnapshotFilter filter,
                                                            Map<String, SortingOrder> sortBy,
                                                            Integer pageNumber,
                                                            Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        String snapshotWhere = "";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sortByKey = sortBy.entrySet()
                .stream()
                .findFirst()
                .map(entry -> {
                    if (SNAPSHOT_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "snapshot_id";
                })
                .orElse("snapshot_id");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.ASC);

        Map<String, List<String>> conditions = createSnapshotWhereClauseAndUpdateParams(params, filter);
        if (conditions.get(SNAPSHOTS_TABLE).size() > 0)
            snapshotWhere = " WHERE " + String.join(" AND ", conditions.get(SNAPSHOTS_TABLE));

        List<DbCoveritySnapshot> results = List.of();
        String sql = "";
        String pagination = " OFFSET :skip LIMIT :limit";
        if (pageSize > 0) {
            sql = "SELECT *"
                    + " FROM " + company + "." + SNAPSHOTS_TABLE + snapshotWhere + " ORDER BY " + sortByKey + " " + sortOrder;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql + pagination, params, DbCoverityConverters.snapshotRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbCoverityStream> listStreams(String company,
                                                        CoverityStreamFilter filter,
                                                        Map<String, SortingOrder> sortBy,
                                                        Integer pageNumber,
                                                        Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String streamsWhere = "";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sortByKey = sortBy.entrySet()
                .stream()
                .findFirst()
                .map(entry -> {
                    if (STREAM_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "name";
                })
                .orElse("name");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.ASC);

        Map<String, List<String>> conditions = createStreamWhereClauseAndUpdateParams(params, filter);
        if (conditions.get(STREAMS_TABLE).size() > 0)
            streamsWhere = " WHERE " + String.join(" AND ", conditions.get(STREAMS_TABLE));

        List<DbCoverityStream> results = List.of();
        String sql = "";
        String pagination = " OFFSET :skip LIMIT :limit";
        if (pageSize > 0) {
            sql = "SELECT *"
                    + " FROM " + company + "." + STREAMS_TABLE + streamsWhere + " ORDER BY " + sortByKey + " " + sortOrder;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql + pagination, params, DbCoverityConverters.streamRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> defectsStackedGroupBy(String company,
                                                                     CoverityDefectFilter filter,
                                                                     List<CoverityDefectFilter.DISTINCT> stacks,
                                                                     Map<String, SortingOrder> sortBy) throws SQLException {
        DbListResponse<DbAggregationResult> result = groupByAndCalculateDefectsCount(company, filter, sortBy);
        if (stacks == null
                || stacks.size() == 0
                || !defectsStackSupported.contains(stacks.get(0)))
            return result;
        CoverityDefectFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        CoverityDefectFilter newFilter;
        for (DbAggregationResult row : result.getRecords()) {
            switch (filter.getAcross()) {
                case impact:
                    newFilter = filter.toBuilder().impacts(List.of(row.getKey())).across(stack).build();
                    break;
                case kind:
                    newFilter = filter.toBuilder().kinds(List.of(row.getKey())).across(stack).build();
                    break;
                case domain:
                    newFilter = filter.toBuilder().domains(List.of(row.getKey())).across(stack).build();
                    break;
                case category:
                    newFilter = filter.toBuilder().categories(List.of(row.getKey())).across(stack).build();
                    break;
                case first_detected:
                case last_detected:
                case snapshot_created:
                    newFilter = getFilterForTrendStack(
                            filter.toBuilder(), row, filter.getAcross(), stack, MoreObjects.firstNonNull(filter.getAggInterval(), "")).build();
                    break;
                case first_detected_stream:
                    newFilter = filter.toBuilder().firstDetectedStreams(List.of(row.getAdditionalKey())).across(stack).build();
                    break;
                case last_detected_stream:
                    newFilter = filter.toBuilder().lastDetectedStreams(List.of(row.getAdditionalKey())).across(stack).build();
                    break;
                case type:
                    newFilter = filter.toBuilder().types(List.of(row.getKey())).across(stack).build();
                    break;
                case checker_name:
                    newFilter = filter.toBuilder().checkerNames(List.of(row.getKey())).across(stack).build();
                    break;
                case component_name:
                    newFilter = filter.toBuilder().componentNames(List.of(row.getKey())).across(stack).build();
                    break;
                case file:
                    newFilter = filter.toBuilder().filePaths(List.of(row.getKey())).across(stack).build();
                    break;
                case function:
                    newFilter = filter.toBuilder().functionNames(List.of(row.getKey())).across(stack).build();
                    break;
                default:
                    throw new UnsupportedOperationException("Stack is not supported for:" + stack);
            }
            finalList.add(row.toBuilder().stacks(groupByAndCalculateDefectsCount(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    public DbListResponse<DbAggregationResult> snapshotsStackedGroupBy(String company,
                                                                       CoveritySnapshotFilter filter,
                                                                       List<CoveritySnapshotFilter.DISTINCT> stacks,
                                                                       Map<String, SortingOrder> sortBy) throws SQLException {
        DbListResponse<DbAggregationResult> result = groupByAndCalculateSnapshotsCount(company, filter, sortBy);
        if (stacks == null
                || stacks.size() == 0
                || !snapshotsStackSupported.contains(stacks.get(0)))
            return result;
        CoveritySnapshotFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        CoveritySnapshotFilter newFilter;
        for (DbAggregationResult row : result.getRecords()) {
            switch (filter.getAcross()) {
                case analysis_host:
                    newFilter = filter.toBuilder().analysisHosts(List.of(row.getKey())).across(stack).build();
                    break;
                case analysis_version:
                    newFilter = filter.toBuilder().analysisVersions(List.of(row.getKey())).across(stack).build();
                    break;
                case commit_user:
                    newFilter = filter.toBuilder().commitUsers(List.of(row.getKey())).across(stack).build();
                    break;
                case snapshot_id:
                    newFilter = filter.toBuilder().snapshotIds(List.of(row.getKey())).across(stack).build();
                    break;
                default:
                    throw new UnsupportedOperationException("Stack is not supported for:" + stack);
            }
            finalList.add(row.toBuilder().stacks(groupByAndCalculateSnapshotsCount(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateDefectsCount(String company,
                                                                               CoverityDefectFilter filter,
                                                                               Map<String, SortingOrder> sortBy) {
        return groupByAndCalculateDefectsCount(company, filter, sortBy, false);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateDefectsCount(String company,
                                                                               CoverityDefectFilter filter,
                                                                               Map<String, SortingOrder> sortBy,
                                                                               boolean isValues) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        CoverityDefectFilter.DISTINCT across = filter.getAcross();
        CoverityDefectFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        String calculationComponent = "";
        String selectDistinctString, groupByString, orderByString = "";
        String defectsWhere = "";
        String intervalColumn = "";
        String streamColumn = "";
        String snapshotColumn = "";
        boolean needTrend = isValues;
        Map<String, List<String>> conditions;
        if (calculation == null)
            calculation = CoverityDefectFilter.CALCULATION.count;
        Optional<String> additionalKey = Optional.empty();
        if(filter.getSnapshotCreatedRange() != null)
            needTrend = needTrend || (filter.getSnapshotCreatedRange().getLeft() != null || filter.getSnapshotCreatedRange().getRight() != null);
        switch (calculation) {
            case count:
                calculationComponent = "COUNT(DISTINCT cid) as ct";
                orderByString = "ct ASC";
                break;
        }
        String key = "";
        switch (across) {
            case type:
            case kind:
            case impact:
            case domain:
            case category:
            case component_name:
            case checker_name:
                groupByString = across.toString();
                selectDistinctString = across.toString();
                break;
            case first_detected_stream:
            case last_detected_stream:
                selectDistinctString = "stream_id," + across;
                groupByString = across + "," + selectDistinctString;
                additionalKey = Optional.of(across.toString());
                streamColumn = ",streams.id as stream_id";
                break;
            case first_detected:
            case last_detected:
                AggTimeQueryHelper.AggTimeQuery issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(across + "_at", across.toString(), filter.getAggInterval(),
                                false);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = issueModAggQuery.getGroupBy();
                orderByString = issueModAggQuery.getOrderBy();
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                break;
            case snapshot_created:
                AggTimeQueryHelper.AggTimeQuery trendAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("EXTRACT(EPOCH FROM snapshot_created_at)", across.toString(), filter.getAggInterval(),
                                true);
                intervalColumn = trendAggQuery.getHelperColumn();
                groupByString = trendAggQuery.getGroupBy();
                orderByString = trendAggQuery.getOrderBy();
                selectDistinctString = trendAggQuery.getSelect();
                additionalKey = Optional.of(trendAggQuery.getIntervalKey());
                snapshotColumn = ",snapshots.snapshot_created_at ";
                needTrend = true;
                break;
            case file:
                groupByString = across.toString();
                selectDistinctString = "file_path AS file";
                break;
            case function:
                groupByString = across.toString();
                selectDistinctString = "function_name AS function";
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        if (MapUtils.isNotEmpty(sortBy)) {
            orderByString = getDefectOrderBy(sortBy, across, calculation);
        }

        conditions = createDefectWhereClauseAndUpdateParams(params, filter, "defects.");
        if (conditions.get(DEFECTS_TABLE).size() > 0)
            defectsWhere = " AND " + String.join(" AND ", conditions.get(DEFECTS_TABLE));

        String snapshotQueryWhere = "";
        Map<String, String> snapshotQuery = getSnapshotQuery(company, params, filter.getSnapshotCreatedAt(), needTrend);
        snapshotQueryWhere = " WHERE " + String.join(" AND ", snapshotQuery.get("snapshot_query"));

        String baseWithClause = "WITH defects AS ";
        String baseWithSql = baseWithClause
                + "( SELECT defects.*"
                + snapshotColumn
                + streamColumn + " FROM "
                + company + "." + DEFECTS_TABLE + " AS defects, " + company + "." + SNAPSHOTS_TABLE + " AS snapshots, " + company + "." + STREAMS_TABLE + " AS streams "
                + snapshotQueryWhere
                + defectsWhere
                + " )";
        String sql = baseWithSql + " SELECT " + selectDistinctString + "," + calculationComponent
                + " FROM ( SELECT * " + intervalColumn + " FROM defects ) y GROUP BY " + groupByString + " ORDER BY " + orderByString;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        key = (List.of("first_detected_stream", "last_detected_stream").contains(across.toString())) ? "stream_id" : across.toString();
        List<DbAggregationResult> results = template.query(sql, params, DbCoverityConverters.distinctRowMapper(key, calculation, additionalKey));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateSnapshotsCount(String company,
                                                                                 CoveritySnapshotFilter filter,
                                                                                 Map<String, SortingOrder> sortBy) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        CoveritySnapshotFilter.DISTINCT across = filter.getAcross();
        CoveritySnapshotFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        String calculationComponent = "";
        String orderByString = "";
        String selectDistinctString, groupByString;
        String snapshotWhere = "";
        Map<String, List<String>> conditions;
        if (calculation == null)
            calculation = CoveritySnapshotFilter.CALCULATION.count;
        conditions = createSnapshotWhereClauseAndUpdateParams(params, filter);
        switch (calculation) {
            case analysis_time:
                calculationComponent = "MIN(time_taken) AS mn,MAX(time_taken) AS mx,COUNT(id) AS ct,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY time_taken) AS median";
                orderByString = "mx DESC";
                break;
            case count:
                calculationComponent = "COUNT(*) as ct";
                orderByString = "ct ASC";
                break;
        }
        switch (across) {
            case commit_user:
            case snapshot_id:
            case analysis_host:
            case analysis_version:
                groupByString = across.toString();
                selectDistinctString = across.toString();
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        if (MapUtils.isNotEmpty(sortBy)) {
            orderByString = getSnapshotOrderBy(sortBy, across, calculation);
        }

        if (conditions.get(SNAPSHOTS_TABLE).size() > 0)
            snapshotWhere = " WHERE " + String.join(" AND ", conditions.get(SNAPSHOTS_TABLE));

        String sql = "SELECT " + selectDistinctString + "," + calculationComponent
                + " FROM ( SELECT * FROM " + company + "." + SNAPSHOTS_TABLE + snapshotWhere + " ) y GROUP BY " + groupByString + " ORDER BY " + orderByString;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbCoverityConverters.snapshotRowMapper(across.toString(), calculation));
        return DbListResponse.of(results, results.size());
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    public HashMap<String, String> simplifyAttributes(List<CoverityAttributes> attributes) {
        HashMap<String, String> result = new HashMap<>();
        attributes.forEach(attribute -> result.put(attribute.getAttributeDefinitionId().getName(), attribute.getAttributeValueId().getName()));
        return result;
    }

    public Map<String, String> getSnapshotQuery(String company, Map<String, Object> params, Long snapshotCreatedAt, boolean needTrend) {
        Map<String, String> andQueryConditions = new HashMap<>();
        if (snapshotCreatedAt != null) {
            andQueryConditions.put("snapshot_query", "snapshots.snapshot_created_at  = TO_TIMESTAMP( :snapshot_created_at ) AND snapshots.id = defects.snapshot_id AND " +
                    "defects.first_detected_stream = streams.name AND snapshots.stream_id = streams.id");
            params.put("snapshot_created_at", snapshotCreatedAt);
        } else {
            String snapshotCondition = (needTrend) ? "" : "snapshots.snapshot_created_at = (SELECT TO_TIMESTAMP(MAX(extract(epoch from snapshot_created_at)))" +
                    " FROM " + company + "." + SNAPSHOTS_TABLE + ") AND ";
            andQueryConditions.put("snapshot_query", snapshotCondition +" snapshots.id = defects.snapshot_id AND defects.first_detected_stream = streams.name AND snapshots.stream_id = streams.id");
        }
        return andQueryConditions;
    }

    private CoverityDefectFilter.CoverityDefectFilterBuilder getFilterForTrendStack(CoverityDefectFilter.CoverityDefectFilterBuilder coverityDefectFilterBuilder,
                                                                                    DbAggregationResult row, CoverityDefectFilter.DISTINCT across,
                                                                                    CoverityDefectFilter.DISTINCT stack, String aggInterval) throws SQLException {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if (aggInterval.equals(AGG_INTERVAL.month.toString()))
            cal.add(Calendar.MONTH, 1);
        else if (aggInterval.equals(AGG_INTERVAL.day.toString()))
            cal.add(Calendar.DATE, 1);
        else if (aggInterval.equals(AGG_INTERVAL.year.toString()))
            cal.add(Calendar.YEAR, 1);
        else if (aggInterval.equals(AGG_INTERVAL.quarter.toString()))
            cal.add(Calendar.MONTH, 3);
        else
            cal.add(Calendar.DATE, 7);
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());

        ImmutablePair<Long, Long> timeRange = ImmutablePair.of(startTimeInSeconds, endTimeInSeconds);
        switch (across) {
            case first_detected:
                coverityDefectFilterBuilder.firstDetectedAt(timeRange);
                break;
            case last_detected:
                coverityDefectFilterBuilder.lastDetectedAt(timeRange);
                break;
            case snapshot_created:
                coverityDefectFilterBuilder.snapshotCreatedAt(startTimeInSeconds);
                break;
            default:
                throw new SQLException("This across option is not available trend. Provided across: " + across);
        }

        return coverityDefectFilterBuilder.across(stack);
    }

    private String getDefectOrderBy(Map<String, SortingOrder> sort, CoverityDefectFilter.DISTINCT across,
                                    CoverityDefectFilter.CALCULATION calculation) {
        String groupByField = sort.keySet().stream().findFirst().get();
        SortingOrder sortOrder = sort.values().stream().findFirst().get();
        if (!across.toString().equals(groupByField)) {
            if (!calculation.toString().equals(groupByField))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + groupByField);
            switch (calculation) {
                case count:
                    return "ct " + sortOrder + " NULLS LAST";
            }
        }
        if (List.of(CoverityDefectFilter.DISTINCT.first_detected, CoverityDefectFilter.DISTINCT.last_detected,
                CoverityDefectFilter.DISTINCT.snapshot_created).contains(across)) {
            return across + "_interval " + sortOrder + " NULLS LAST";
        } else if (across.equals(CoverityDefectFilter.DISTINCT.impact)) {
            sortOrder = (sortOrder.equals(SortingOrder.ASC)) ? SortingOrder.DESC : SortingOrder.ASC;
            return "LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST";
        } else if (List.of(CoverityDefectFilter.DISTINCT.file, CoverityDefectFilter.DISTINCT.function).contains(across))
            return groupByField + " " + sortOrder + " NULLS LAST";
        return "LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST";
    }

    private String getSnapshotOrderBy(Map<String, SortingOrder> sort, CoveritySnapshotFilter.DISTINCT across,
                                      CoveritySnapshotFilter.CALCULATION calculation) {
        String groupByField = sort.keySet().stream().findFirst().get();
        SortingOrder sortOrder = sort.values().stream().findFirst().get();
        if (!across.toString().equals(groupByField)) {
            if (!calculation.toString().equals(groupByField))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + groupByField);
            switch (calculation) {
                case analysis_time:
                    return "median " + sortOrder + " NULLS LAST";
                case count:
                    return "ct " + sortOrder + " NULLS LAST";
            }
        }
        if (across.equals(CoveritySnapshotFilter.DISTINCT.snapshot_id))
            return groupByField + " " + sortOrder + " NULLS LAST";
        return " LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST";
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + STREAMS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    language VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    triage_store_id VARCHAR,\n" +
                        "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    UNIQUE (name,project,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + STREAMS_TABLE + "_name_project_integration_compound_idx " +
                        "on " + company + "." + STREAMS_TABLE + "(name,project,integration_id)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + SNAPSHOTS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    stream_id UUID REFERENCES " +
                        company + "." + STREAMS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    snapshot_id INTEGER NOT NULL,\n" +
                        "    analysis_host VARCHAR NOT NULL,\n" +
                        "    analysis_version VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    time_taken INTEGER NOT NULL,\n" +
                        "    build_failure_count INTEGER,\n" +
                        "    build_success_count INTEGER,\n" +
                        "    commit_user VARCHAR NOT NULL,\n" +
                        "    snapshot_created_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                        "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    UNIQUE (snapshot_id,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + SNAPSHOTS_TABLE + "_snapshot_integration_compound_idx " +
                        "on " + company + "." + SNAPSHOTS_TABLE + "(snapshot_id,integration_id)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + DEFECTS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    snapshot_id UUID REFERENCES " +
                        company + "." + SNAPSHOTS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    cid INTEGER NOT NULL,\n" +
                        "    checker_name VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    component_name VARCHAR NOT NULL,\n" +
                        "    cwe INTEGER NOT NULL,\n" +
                        "    attributes JSONB,\n" +
                        "    category VARCHAR NOT NULL,\n" +
                        "    impact VARCHAR NOT NULL,\n" +
                        "    kind VARCHAR,\n" +
                        "    type VARCHAR,\n" +
                        "    domain VARCHAR,\n" +
                        "    file_path VARCHAR NOT NULL,\n" +
                        "    function_name VARCHAR,\n" +
                        "    first_detected_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                        "    first_detected_by VARCHAR,\n" +
                        "    first_detected_stream VARCHAR NOT NULL,\n" +
                        "    first_detected_snapshot_id INTEGER,\n" +
                        "    last_detected_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                        "    last_detected_stream VARCHAR,\n" +
                        "    last_detected_snapshot_id INTEGER,\n" +
                        "    merge_key VARCHAR,\n" +
                        "    misra_category VARCHAR,\n" +
                        "    occurrence_count INTEGER,\n" +
                        "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),\n" +
                        "    UNIQUE (cid,snapshot_id,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + DEFECTS_TABLE + "_cid_snapshot_integration_compound_idx " +
                        "on " + company + "." + DEFECTS_TABLE + "(cid,snapshot_id,integration_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
