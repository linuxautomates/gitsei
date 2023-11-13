package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService.CiCdJobRunArtifactFilter;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.utils.SetUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact.CONTAINER_TYPE;

@Log4j2
@Service
public class CicdJobRunArtifactCorrelationService {

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunArtifactMappingDatabaseService mappingDatabaseService;
    private final CiCdJobRunArtifactsDatabaseService artifactsDatabaseService;
    private CicdArtifactCorrelationSettings correlationSettings;
    private static final int DEFAULT_READ_PAGE_SIZE = 10000;
    private static final int DEFAULT_WRITE_PAGE_SIZE = 500;
    private final int readPageSize;
    private final int writePageSize;

    @Autowired
    public CicdJobRunArtifactCorrelationService(DataSource dataSource,
                                                CiCdJobRunArtifactMappingDatabaseService mappingDatabaseService,
                                                CicdArtifactCorrelationSettings correlationSettings,
                                                CiCdJobRunArtifactsDatabaseService artifactsDatabaseService
    ) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.mappingDatabaseService = mappingDatabaseService;
        this.artifactsDatabaseService = artifactsDatabaseService;
        this.correlationSettings = correlationSettings;
        this.readPageSize = MoreObjects.firstNonNull(correlationSettings.getReadPageSize(), DEFAULT_READ_PAGE_SIZE);
        this.writePageSize = MoreObjects.firstNonNull(correlationSettings.getWritePageSize(), DEFAULT_WRITE_PAGE_SIZE);
    }

    /**
     * Stores various settings related to which correlations are enabled:
     * - if correlation{Name}Default is True, then the correlation {Name} will be enabled by default for all tenants
     * - otherwise, only if correlation{Name}Tenants, a comma-separated list, contains a given tenant, then the correlation will be enabled.
     */
    @Value
    @Builder(toBuilder = true)
    public static class CicdArtifactCorrelationSettings {
        Integer readPageSize;
        Integer writePageSize;
        boolean correlationIdentityDefault;
        Set<String> correlationIdentityTenants;
        boolean correlationNameQualifierDefault;
        Set<String> correlationNameQualifierTenants;
        boolean correlationNameQualifierLocationDefault;
        Set<String> correlationNameQualifierLocationTenants;
        boolean correlationHashDefault;
        Set<String> correlationHashTenants;

        public List<Correlation> getCorrelationsForTenant(String company) {
            List<Correlation> correlations = new ArrayList<>();
            if (correlationIdentityDefault || SetUtils.contains(correlationIdentityTenants, company)) {
                correlations.add(Correlation.IDENTITY);
            }
            if (correlationNameQualifierDefault || SetUtils.contains(correlationNameQualifierTenants, company)) {
                correlations.add(Correlation.NAME_QUALIFIER);
            }
            if (correlationNameQualifierLocationDefault || SetUtils.contains(correlationNameQualifierLocationTenants, company)) {
                correlations.add(Correlation.NAME_QUALIFIER_LOCATION);
            }
            if (correlationHashDefault || SetUtils.contains(correlationHashTenants, company)) {
                correlations.add(Correlation.HASH);
            }

            return correlations;
        }
    }

    /**
     * SQL for each supported correlation.
     * Note that if multiple correlations are enabled, a UNION between them will be taken.
     * Therefore, the order of the columns must be respected (run_id, artifact_id).
     */
    public enum Correlation {
        /**
         * This identity correlation maps every job run to itself even when there is no artifact.
         * This can be used to :
         * 1) ensure unwanted correlations are cleaned up (otherwise only job runs that have matches would be processed)
         * 2) mapping jobs to themselves even if they don't have artifacts (for backward compatibility maybe? or jobs that have both CI+CD but somehow no artifact)
         */
        IDENTITY("" +
                " SELECT ARRAY[id]::UUID[] AS run_id, ARRAY[]::UUID[] AS artifact_id" +
                " FROM (SELECT id FROM ${company}.cicd_job_runs) AS job_runs", a -> null),
        /**
         * Containers that share the same name & qualifier (for example, image:v1)
         * even if they are in different locations (meaning, different image repositories).
         * <br/>
         * Note: since this is less restrictive than NAME_QUALIFIER_LOCATION, this correlation should only be enabled if LOCATION data is not good quality.
         */
        NAME_QUALIFIER(" SELECT ARRAY_AGG(cicd_job_run_id) AS run_id, array_agg(id) AS artifact_id " +
                " FROM ${company}.cicd_job_run_artifacts" +
                " WHERE type = '${container_type}' " +
                " AND name != '' " +
                " AND qualifier != '' " +
                " GROUP BY name, qualifier",
                Correlation::nameQualifierFilter),
        /**
         * Same as NAME_QUALIFIER with the addition of LOCATION. For containers, it means that the image repository must match as well.
         */
        NAME_QUALIFIER_LOCATION(
                " SELECT ARRAY_AGG(cicd_job_run_id) AS run_id, array_agg(id) AS artifact_id " +
                        " FROM ${company}.cicd_job_run_artifacts" +
                        " WHERE type = '${container_type}' " +
                        " AND name != '' " +
                        " AND qualifier != '' " +
                        " AND location != '' " +
                        " GROUP BY name, qualifier, location",
                Correlation::nameQualifierLocationFilter
        ),
        /**
         * Containers that share the same hash, regardless of any other metadata.
         */
        HASH(" SELECT ARRAY_AGG(cicd_job_run_id) AS run_id, array_agg(id) AS artifact_id " +
                " FROM ${company}.cicd_job_run_artifacts " +
                " WHERE type = '${container_type}' " +
                " AND hash != '' " +
                " GROUP BY hash",
                Correlation::hashFilter);

        private final String sql;
        private final Function<CiCdJobRunArtifact, CiCdJobRunArtifactFilter> individualFilterFunction;

        Correlation(String sql, Function<CiCdJobRunArtifact, CiCdJobRunArtifactFilter> individualFilterFunction) {
            this.sql = sql;
            this.individualFilterFunction = individualFilterFunction;
        }

        public String getSql(String company) {
            return StringSubstitutor.replace(sql, Map.of(
                    "company", company,
                    "container_type", CONTAINER_TYPE));
        }

        public CiCdJobRunArtifactFilter getIndividualFilter(CiCdJobRunArtifact artifact) {
            return individualFilterFunction.apply(artifact);
        }

        public static CiCdJobRunArtifactFilter nameQualifierFilter(
                CiCdJobRunArtifact artifact
        ) {
            return CiCdJobRunArtifactFilter.builder()
                    .names(List.of(artifact.getName()))
                    .qualifiers(List.of(artifact.getQualifier()))
                    .excludeIds(List.of(artifact.getId()))
                    .types(List.of(CONTAINER_TYPE))
                    .build();
        }

        public static CiCdJobRunArtifactFilter nameQualifierLocationFilter(
                CiCdJobRunArtifact artifact
        ) {
            return CiCdJobRunArtifactFilter.builder()
                    .names(List.of(artifact.getName()))
                    .qualifiers(List.of(artifact.getQualifier()))
                    .excludeIds(List.of(artifact.getId()))
                    .locations(List.of(artifact.getLocation()))
                    .types(List.of(CONTAINER_TYPE))
                    .build();
        }

        public static CiCdJobRunArtifactFilter hashFilter(
                CiCdJobRunArtifact artifact
        ) {
            return CiCdJobRunArtifactFilter.builder()
                    .hashes(List.of(artifact.getHash()))
                    .excludeIds(List.of(artifact.getId()))
                    .types(List.of(CONTAINER_TYPE))
                    .build();
        }
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IntermediateMapping.IntermediateMappingBuilder.class)
    public static class IntermediateMapping {
        @JsonProperty("run_id1")
        UUID runId1;
        @JsonProperty("run_ids")
        Set<UUID> runIds;
    }

    public long correlateAndUpdateArtifactMappings(String company) {
        MutableLong nbArtifacts = new MutableLong(0);
        try {
            Stream<IntermediateMapping> intermediateMappingStream = streamCorrelatedArtifacts(company);
            StreamUtils.forEachPage(intermediateMappingStream, writePageSize, page -> {
                mappingDatabaseService.bulkReplace(company, page);
                nbArtifacts.add(page.size());
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to correlate or upsert artifacts after " + nbArtifacts.longValue() + " records", e);
        }
        return nbArtifacts.longValue();
    }

    protected Stream<IntermediateMapping> streamCorrelatedArtifacts(String company) {
        return PaginationUtils.stream(0, 1, pageNumber -> getCorrelatedArtifacts(company, pageNumber, readPageSize));
    }

    protected List<IntermediateMapping> getCorrelatedArtifacts(String company, int pageNumber, int pageSize) {
        Validate.notBlank(company, "company cannot be null or empty.");
        int limit = MoreObjects.firstNonNull(pageSize, readPageSize);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<Correlation> correlations = correlationSettings.getCorrelationsForTenant(company);
        if (correlations.isEmpty()) {
            log.warn("All correlations have been disabled!");
            return List.of();
        }
        log.info("correlations={}", correlations);

        List<String> correlationsSql = correlations.stream()
                .map(correlation -> correlation.getSql(company))
                .collect(Collectors.toList());
        ;
        String sql = "" +
                " SELECT run_id1, array_agg(distinct run_id2) AS run_ids FROM ( " +
                "   SELECT run_id1, unnest(run_ids) AS run_id2 FROM ( " +
                "     SELECT unnest(run_id) AS run_id1, run_id AS run_ids FROM ( " +
                "       SELECT run_id, artifact_id FROM (" +
                "         (" + String.join(") UNION (", correlationsSql) + ")" +
                "       ) AS t " +
                "     ) AS t2 " +
                "     ORDER BY run_id " +
                "   ) AS t3 " +
                " ) AS t4 " +
                " GROUP BY run_id1 " +
                " OFFSET :skip " +
                " LIMIT :limit ";

        Map<String, Integer> params = Map.of("skip", skip, "limit", limit);

        return template.query(sql, params, ((rs, rowNum) -> IntermediateMapping.builder()
                .runId1(rs.getObject("run_id1", UUID.class))
                .runIds(DatabaseUtils.fromSqlArray(rs.getArray("run_ids"), UUID.class).collect(Collectors.toSet()))
                .build()));
    }

    public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private List<CiCdJobRunArtifact> getArtifiactsFromIds(String company, List<String> artifactIds) {
        return artifactsDatabaseService.stream(
                        company,
                        CiCdJobRunArtifactFilter.builder()
                                .ids(artifactIds)
                                .build())
                .collect(Collectors.toList());
    }

    private List<CiCdJobRunArtifact> getRelatedArtifacts(String company, List<CiCdJobRunArtifact> artifacts) {
        List<Correlation> correlations = correlationSettings.getCorrelationsForTenant(company);
        return artifacts.stream().flatMap(artifact -> {
            return correlations.stream()
                    .map(correlation -> correlation.getIndividualFilter(artifact))
                    .filter(Objects::nonNull)
                    .flatMap(filter -> artifactsDatabaseService.stream(company, filter))
                    .filter(distinctByKey(CiCdJobRunArtifact::getId));
        }).collect(Collectors.toList());
    }

    protected List<IntermediateMapping> getIndividualCorrelatedArtifacts(String company, CICDJobRun cicdJobRun, List<String> artifactIds) {
        List<CiCdJobRunArtifact> artifactList = artifactIds.size() > 0 ? getArtifiactsFromIds(company, artifactIds) : List.of();
        List<Correlation> correlations = correlationSettings.getCorrelationsForTenant(company);

        List<CiCdJobRunArtifact> relatedArtifacts = getRelatedArtifacts(company, artifactList);
        Set<UUID> relatedCicdJobRunIds = relatedArtifacts.stream()
                .map(CiCdJobRunArtifact::getCicdJobRunId)
                .collect(Collectors.toSet());

        // Build correlations
        List<IntermediateMapping> intermediateMappings = new ArrayList<>();

        // Add the correlations we found above
        if (relatedCicdJobRunIds.size() > 0) {
            intermediateMappings.add(IntermediateMapping.builder()
                    .runId1(cicdJobRun.getId())
                    .runIds(relatedCicdJobRunIds)
                    .build());
        }

        // Add the identity correlation
        if (correlations.contains(Correlation.IDENTITY)) {
            intermediateMappings.add(IntermediateMapping.builder()
                    .runId1(cicdJobRun.getId())
                    .runIds(Set.of(cicdJobRun.getId()))
                    .build());
        }

        // Add the reverse correlations
        relatedCicdJobRunIds.forEach(runId -> {
            intermediateMappings.add(IntermediateMapping.builder()
                    .runId1(runId)
                    .runIds(Set.of(cicdJobRun.getId()))
                    .build());
        });
        log.debug("Total artifact correlations found: {}", intermediateMappings.size());
        return intermediateMappings;
    }

    public void mapCicdJob(String company, CICDJobRun cicdJobRun, List<String> artifactIds) {
        try {
            List<IntermediateMapping> intermediateMappings = getIndividualCorrelatedArtifacts(company, cicdJobRun, artifactIds);
            mappingDatabaseService.bulkReplace(company, intermediateMappings);
        } catch (Exception e) {
            log.error("Failed to map cicd job run {}", cicdJobRun.getId(), e);
        }
    }

}
