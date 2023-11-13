package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.coverity.DbCoverityDefect;
import io.levelops.commons.databases.models.database.coverity.DbCoveritySnapshot;
import io.levelops.commons.databases.models.database.coverity.DbCoverityStream;
import io.levelops.commons.databases.models.filters.CoverityDefectFilter;
import io.levelops.commons.databases.models.filters.CoveritySnapshotFilter;
import io.levelops.commons.databases.models.filters.CoverityStreamFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class CoverityDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static CoverityDatabaseService coverityDatabaseService;
    private static IntegrationService integrationService;
    private static String id;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
        coverityDatabaseService = new CoverityDatabaseService(dataSource);

        integrationService.ensureTableExistence(company);
        coverityDatabaseService.ensureTableExistence(company);
        id = integrationService.insert(company, Integration.builder()
                .application("coverity")
                .name("coverity test")
                .status("enabled")
                .build());
    }

    public void testInsert() throws IOException, SQLException {
        String input = ResourceUtils.getResourceAsString("json/databases/coverity.json");
        PaginatedResponse<EnrichedProjectData> data = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));

        // stream insertion
        DbCoverityStream dbCoverityStream = DbCoverityStream.fromStream(data.getResponse().getRecords().get(0).getStream(), id);
        DbCoverityStream dbCoverityStream1 = DbCoverityStream.fromStream(data.getResponse().getRecords().get(1).getStream(), id);
        List<String> streamIds = coverityDatabaseService.upsertStream(company, List.of(dbCoverityStream, dbCoverityStream1));
        assertThat(streamIds.size()).isNotNull();
        assertThat(streamIds.size()).isEqualTo(2);

        // snapshot insertion
        DbCoveritySnapshot dbCoveritySnapshot = DbCoveritySnapshot.fromSnapshot(data.getResponse().getRecords().get(0).getSnapshot(), id, streamIds.get(1));
        DbCoveritySnapshot dbCoveritySnapshot1 = DbCoveritySnapshot.fromSnapshot(data.getResponse().getRecords().get(1).getSnapshot(), id, streamIds.get(0));
        List<String> snapshotIds = coverityDatabaseService.upsertSnapshot(company, List.of(dbCoveritySnapshot1, dbCoveritySnapshot));
        assertThat(snapshotIds.size()).isNotNull();
        assertThat(snapshotIds.size()).isEqualTo(2);

        // defects insertion
        List<DbCoverityDefect> defects = data.getResponse().getRecords().get(0).getDefects().stream()
                .map(defect -> DbCoverityDefect.fromDefect(defect, id, snapshotIds.get(0))).collect(Collectors.toList());
        defects.forEach(defect -> {
            try {
                String defectId = coverityDatabaseService.insert(company, defect);
                assertThat(defectId).isNotNull();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void testDefectsList() throws SQLException {
        DbListResponse<DbCoverityDefect> defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .cids(List.of("10001", "10002"))
                        .domains(List.of("STATIC_JAVA"))
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.category.toString(), SortingOrder.DESC),
                0, 100);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(2);
        assertThat(defectsList.getRecords().stream().map(DbCoverityDefect::getCategory).collect(Collectors.toList()))
                .containsAll(List.of("Null pointer dereferences", "Null pointer dereferences"));

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .categories(List.of("Null pointer dereferences"))
                        .impacts(List.of("High"))
                        .kinds(List.of("Quality"))
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.kind.toString(), SortingOrder.ASC),
                0, 100);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(1);
        assertThat(defectsList.getRecords().stream().map(DbCoverityDefect::getKind).collect(Collectors.toList()))
                .containsAll(List.of("Quality"));

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .lastDetectedAt(ImmutablePair.of(1628837000L, 1628838200L))
                        .componentNames(List.of("Default.Other"))
                        .firstDetectedSnapshotIds(List.of("10006"))
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.first_detected.toString(), SortingOrder.DESC),
                0, 100);
        assertThat(defectsList.getRecords()).isEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(0);

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .excludeCids(List.of("10001"))
                        .excludeKinds(List.of("various"))
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.domain.toString(), SortingOrder.DESC),
                0, 100);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(2);
        assertThat(defectsList.getRecords().stream().map(DbCoverityDefect::getDomain).collect(Collectors.toList()))
                .containsAll(List.of("STATIC_JAVA", "STATIC_JAVA"));

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .excludeKinds(List.of("various"))
                        .build(),
                Map.of("cid", SortingOrder.ASC),
                0, 100);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(3);
        assertThat(defectsList.getRecords().stream().map(DbCoverityDefect::getCwe).collect(Collectors.toList()))
                .containsAll(List.of(10001, 10002, 10003));
        assertThat(defectsList.getRecords().get(0).getDbAttributes()).isNotEmpty();

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedAt(1628860728L)
                        .excludeDomains(List.of("STATIC_JAVA"))
                        .excludeFirstDetectedSnapshotIds(List.of("10006"))
                        .excludeCheckerNames(List.of("NULL_RETURNS"))
                        .excludeCategories(List.of("Null pointer dereferences"))
                        .excludeCids(List.of("10002"))
                        .excludeKinds(List.of("various"))
                        .build(),
                Map.of(),
                0, 100);
        assertThat(defectsList.getRecords()).isEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(0);

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedRange(ImmutablePair.of(1628862100L,1628862201L))
                        .build(),
                Map.of(),
                0, 100);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getTotalCount()).isEqualTo(3);

        defectsList = coverityDatabaseService.list(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedRange(ImmutablePair.of(1628862100L,1628862201L))
                        .build(),
                Map.of(),
                1, 2);
        assertThat(defectsList.getRecords()).isNotEmpty();
        assertThat(defectsList.getCount()).isEqualTo(1);
        assertThat(defectsList.getTotalCount()).isEqualTo(3);
    }

    public void testStreamsList() throws SQLException {
        DbListResponse<DbCoverityStream> streamsList = coverityDatabaseService.listStreams(company,
                CoverityStreamFilter.builder()
                        .integrationIds(List.of(id))
                        .languages(List.of("MIXED"))
                        .projects(List.of("New Project 1"))
                        .build(),
                Map.of("name", SortingOrder.ASC),
                0, 100);
        assertThat(streamsList.getRecords()).isNotEmpty();
        assertThat(streamsList.getTotalCount()).isEqualTo(1);
        assertThat(streamsList.getRecords().stream().map(DbCoverityStream::getName).collect(Collectors.toList()))
                .containsAll(List.of("trigger"));
        streamsList = coverityDatabaseService.listStreams(company,
                CoverityStreamFilter.builder()
                        .excludeTriageStoreIds(List.of("Default Triage Store"))
                        .excludeLanguages(List.of("MIXED"))
                        .excludeNames(List.of("trigger"))
                        .integrationIds(List.of(id))
                        .build(),
                Map.of("project", SortingOrder.DESC),
                0, 100);
        assertThat(streamsList.getRecords()).isEmpty();
        assertThat(streamsList.getTotalCount()).isEqualTo(0);
        streamsList = coverityDatabaseService.listStreams(company,
                CoverityStreamFilter.builder()
                        .excludeNames(List.of("trigger"))
                        .names(List.of("test"))
                        .integrationIds(List.of(id))
                        .build(),
                Map.of("language", SortingOrder.DESC),
                0, 100);
        assertThat(streamsList.getRecords()).isNotEmpty();
        assertThat(streamsList.getTotalCount()).isEqualTo(1);
        assertThat(streamsList.getRecords().stream().map(DbCoverityStream::getLanguage).collect(Collectors.toList()))
                .containsAll(List.of("MIXED"));
        streamsList = coverityDatabaseService.listStreams(company,
                CoverityStreamFilter.builder()
                        .integrationIds(List.of(id))
                        .build(),
                Map.of("language", SortingOrder.DESC),
                0, 1);
        assertThat(streamsList.getRecords()).isNotEmpty();
        assertThat(streamsList.getCount()).isEqualTo(1);
        assertThat(streamsList.getTotalCount()).isEqualTo(2);

    }

    public void testSnapshotsList() throws SQLException {
        DbListResponse<DbCoveritySnapshot> snapshotsList = coverityDatabaseService.listSnapshots(company,
                CoveritySnapshotFilter.builder()
                        .analysisHosts(List.of("synopsis-dev"))
                        .analysisVersions(List.of("2021.06"))
                        .snapshotIds(List.of("10006", "10005"))
                        .build(),
                Map.of("analysis_host", SortingOrder.DESC),
                0, 100);
        assertThat(snapshotsList.getRecords()).isNotEmpty();
        assertThat(snapshotsList.getTotalCount()).isEqualTo(2);
        assertThat(snapshotsList.getRecords().stream().map(DbCoveritySnapshot::getAnalysisHost).collect(Collectors.toList()))
                .containsAll(List.of("synopsis-dev"));

        snapshotsList = coverityDatabaseService.listSnapshots(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .buildFailureCount(ImmutablePair.of(-1L, 1L))
                        .commitUsers(List.of("admin"))
                        .build(),
                Map.of("analysis_version", SortingOrder.ASC),
                0, 100);
        assertThat(snapshotsList.getRecords()).isNotEmpty();
        assertThat(snapshotsList.getTotalCount()).isEqualTo(2);
        assertThat(snapshotsList.getRecords().stream().map(DbCoveritySnapshot::getAnalysisVersion).collect(Collectors.toList()))
                .containsAll(List.of("2021.06"));

        snapshotsList = coverityDatabaseService.listSnapshots(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .excludeAnalysisHosts(List.of("synopsis-dev"))
                        .excludeSnapshotIds(List.of("10006"))
                        .commitUsers(List.of("admin"))
                        .build(),
                Map.of("snapshot_id", SortingOrder.ASC),
                0, 100);
        assertThat(snapshotsList.getRecords()).isEmpty();
        assertThat(snapshotsList.getTotalCount()).isEqualTo(0);

        snapshotsList = coverityDatabaseService.listSnapshots(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .build(),
                Map.of("snapshot_id", SortingOrder.ASC),
                1, 1);
        assertThat(snapshotsList.getRecords()).isNotEmpty();
        assertThat(snapshotsList.getTotalCount()).isEqualTo(2);
        assertThat(snapshotsList.getCount()).isEqualTo(1);
    }

    public void testGroupByAndCalculateSnapshotsCount() throws SQLException {
        assertThat(coverityDatabaseService.groupByAndCalculateSnapshotsCount(company,
                CoveritySnapshotFilter.builder()
                        .across(CoveritySnapshotFilter.DISTINCT.analysis_host)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                Map.of(CoveritySnapshotFilter.DISTINCT.analysis_host.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateSnapshotsCount(company,
                CoveritySnapshotFilter.builder()
                        .analysisVersions(List.of("2021.06"))
                        .snapshotCreatedAt(ImmutablePair.of(1628860300L, 1628862300L))
                        .across(CoveritySnapshotFilter.DISTINCT.snapshot_id)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                Map.of(CoveritySnapshotFilter.CALCULATION.count.toString(), SortingOrder.DESC)).getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.groupByAndCalculateSnapshotsCount(company,
                CoveritySnapshotFilter.builder()
                        .buildFailureCount(ImmutablePair.of(-1L, 1L))
                        .across(CoveritySnapshotFilter.DISTINCT.commit_user)
                        .calculation(CoveritySnapshotFilter.CALCULATION.analysis_time)
                        .build(),
                Map.of(CoveritySnapshotFilter.CALCULATION.analysis_time.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateSnapshotsCount(company,
                CoveritySnapshotFilter.builder()
                        .excludeAnalysisHosts(List.of("synopsis-devs"))
                        .across(CoveritySnapshotFilter.DISTINCT.analysis_version)
                        .calculation(CoveritySnapshotFilter.CALCULATION.analysis_time)
                        .build(),
                Map.of(CoveritySnapshotFilter.DISTINCT.analysis_version.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateSnapshotsCount(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoveritySnapshotFilter.DISTINCT.snapshot_id)
                        .calculation(CoveritySnapshotFilter.CALCULATION.analysis_time)
                        .build(),
                Map.of(CoveritySnapshotFilter.DISTINCT.snapshot_id.toString(), SortingOrder.ASC)).getCount()).isEqualTo(2);
    }

    public void testGroupByAndCalculateDefectsCount() throws SQLException {
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.category)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.category.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .excludeCids(List.of("10001"))
                        .across(CoverityDefectFilter.DISTINCT.checker_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.checker_name.toString(), SortingOrder.DESC)).getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .kinds(List.of("Quality", "various"))
                        .lastDetectedAt(ImmutablePair.of(1628860300L, 1628862300L))
                        .across(CoverityDefectFilter.DISTINCT.domain)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .impacts(List.of("Medium", "High"))
                        .across(CoverityDefectFilter.DISTINCT.component_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.component_name.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .snapshotCreatedRange(ImmutablePair.of(1628862100L,1628862201L))
                        .across(CoverityDefectFilter.DISTINCT.snapshot_created)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.snapshot_created.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .types(List.of("Dereference null return value"))
                        .across(CoverityDefectFilter.DISTINCT.first_detected)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.first_detected.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .snapshotCreatedAt(1628860728L)
                        .excludeCids(List.of("10001"))
                        .across(CoverityDefectFilter.DISTINCT.first_detected_stream)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.DESC)).getCount()).isEqualTo(0);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .excludeKinds(List.of("Various"))
                        .across(CoverityDefectFilter.DISTINCT.last_detected)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.last_detected.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.last_detected_stream)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.last_detected_stream.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .impacts(List.of("Medium", "High"))
                        .across(CoverityDefectFilter.DISTINCT.type)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.type.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .excludeFirstDetectedStreams(List.of("test"))
                        .across(CoverityDefectFilter.DISTINCT.kind)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.kind.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .excludeFirstDetectedSnapshotIds(List.of("10001"))
                        .across(CoverityDefectFilter.DISTINCT.impact)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.impact.toString(), SortingOrder.ASC)).getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.snapshot_created)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.snapshot_created.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.file)
                        .filePaths(List.of("/opt/chatserver/MyServer.java"))
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.file.toString(), SortingOrder.DESC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.file)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.function)
                        .functionNames(List.of("MyClient.actionPerformed"))
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.function.toString(), SortingOrder.ASC)).getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.checker_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.checker_name.toString(), SortingOrder.ASC),
                true).getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.groupByAndCalculateDefectsCount(company,
                CoverityDefectFilter.builder()
                        .across(CoverityDefectFilter.DISTINCT.impact)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                Map.of(CoverityDefectFilter.DISTINCT.impact.toString(), SortingOrder.DESC),
                true).getCount()).isEqualTo(2);
    }

    public void testStackGroupBy() throws SQLException {
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.category)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.type),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.impact)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.checker_name),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC))
                .getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.kind)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.category),
                Map.of(CoverityDefectFilter.DISTINCT.kind.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedAt(1628860728L)
                        .across(CoverityDefectFilter.DISTINCT.last_detected_stream)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.first_detected),
                Map.of(CoverityDefectFilter.DISTINCT.last_detected_stream.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(0);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedAt(1628862200L)
                        .across(CoverityDefectFilter.DISTINCT.last_detected)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.last_detected),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedAt(1628862200L)
                        .across(CoverityDefectFilter.DISTINCT.kind)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.first_detected_stream),
                Map.of())
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .snapshotCreatedAt(1628860728L)
                        .across(CoverityDefectFilter.DISTINCT.domain)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.last_detected_stream),
                Map.of(CoverityDefectFilter.DISTINCT.domain.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(0);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.checker_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.type), null)
                .getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.snapshotsStackedGroupBy(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoveritySnapshotFilter.DISTINCT.analysis_host)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                List.of(CoveritySnapshotFilter.DISTINCT.snapshot_id),
                Map.of(CoveritySnapshotFilter.DISTINCT.analysis_host.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.snapshotsStackedGroupBy(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoveritySnapshotFilter.DISTINCT.snapshot_id)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                List.of(CoveritySnapshotFilter.DISTINCT.commit_user),
                Map.of(CoveritySnapshotFilter.DISTINCT.snapshot_id.toString(), SortingOrder.ASC))
                .getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.snapshotsStackedGroupBy(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoveritySnapshotFilter.DISTINCT.analysis_version)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                List.of(CoveritySnapshotFilter.DISTINCT.analysis_host),
                Map.of(CoveritySnapshotFilter.DISTINCT.analysis_version.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.snapshotsStackedGroupBy(company,
                CoveritySnapshotFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoveritySnapshotFilter.DISTINCT.commit_user)
                        .calculation(CoveritySnapshotFilter.CALCULATION.count)
                        .build(),
                List.of(CoveritySnapshotFilter.DISTINCT.analysis_version), null)
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.type)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.snapshot_created),
                Map.of(CoverityDefectFilter.DISTINCT.type.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.checker_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.file),
                Map.of(CoverityDefectFilter.CALCULATION.count.toString(), SortingOrder.ASC))
                .getCount()).isEqualTo(2);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.component_name)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.function),
                Map.of(CoverityDefectFilter.DISTINCT.component_name.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
        assertThat(coverityDatabaseService.defectsStackedGroupBy(company,
                CoverityDefectFilter.builder()
                        .integrationIds(List.of(id))
                        .across(CoverityDefectFilter.DISTINCT.snapshot_created)
                        .calculation(CoverityDefectFilter.CALCULATION.count)
                        .build(),
                List.of(CoverityDefectFilter.DISTINCT.function),
                Map.of(CoverityDefectFilter.DISTINCT.snapshot_created.toString(), SortingOrder.DESC))
                .getCount()).isEqualTo(1);
    }

    @Test
    public void test() throws SQLException, IOException {
        testInsert();
        testDefectsList();
        testStreamsList();
        testSnapshotsList();
        testGroupByAndCalculateSnapshotsCount();
        testGroupByAndCalculateDefectsCount();
        testStackGroupBy();
    }
}
