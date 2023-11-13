package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
public class BitbucketServerAggHelperTest {

    public static final String arrayUniq = "CREATE OR REPLACE FUNCTION anyarray_uniq(with_array anyarray)\n" +
            "\tRETURNS anyarray AS\n" +
            "$BODY$\n" +
            "\tDECLARE\n" +
            "\t\t-- The variable used to track iteration over \"with_array\".\n" +
            "\t\tloop_offset integer;\n" +
            "\n" +
            "\t\t-- The array to be returned by this function.\n" +
            "\t\treturn_array with_array%TYPE := '{}';\n" +
            "\tBEGIN\n" +
            "\t\tIF with_array IS NULL THEN\n" +
            "\t\t\treturn NULL;\n" +
            "\t\tEND IF;\n" +
            "\t\t\n" +
            "\t\tIF with_array = '{}' THEN\n" +
            "\t\t    return return_array;\n" +
            "\t\tEND IF;\n" +
            "\n" +
            "\t\t-- Iterate over each element in \"concat_array\".\n" +
            "\t\tFOR loop_offset IN ARRAY_LOWER(with_array, 1)..ARRAY_UPPER(with_array, 1) LOOP\n" +
            "\t\t\tIF with_array[loop_offset] IS NULL THEN\n" +
            "\t\t\t\tIF NOT EXISTS(\n" +
            "\t\t\t\t\tSELECT 1 \n" +
            "\t\t\t\t\tFROM UNNEST(return_array) AS s(a)\n" +
            "\t\t\t\t\tWHERE a IS NULL\n" +
            "\t\t\t\t) THEN\n" +
            "\t\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\t\tEND IF;\n" +
            "\t\t\t-- When an array contains a NULL value, ANY() returns NULL instead of FALSE...\n" +
            "\t\t\tELSEIF NOT(with_array[loop_offset] = ANY(return_array)) OR NOT(NULL IS DISTINCT FROM (with_array[loop_offset] = ANY(return_array))) THEN\n" +
            "\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\tEND IF;\n" +
            "\t\tEND LOOP;\n" +
            "\n" +
            "\tRETURN return_array;\n" +
            " END;\n" +
            "$BODY$ LANGUAGE plpgsql;";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Mock
    Storage storage;
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private UserIdentityService userIdentityService;
    private IntegrationTrackingService integrationTrackingService;
    private BitbucketServerAggHelper bitbucketServerAggHelper;
    private static final String company = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Before
    public void setup() throws IOException, URISyntaxException, SQLException {
        MockitoAnnotations.initMocks(this);
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        UserService userService = new UserService(dataSource, OBJECT_MAPPER);
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .id("1")
                .application("gitlab")
                .name("gitlab-integ")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        scmAggService.ensureTableExistence(company);
        JobDtoParser parser = new JobDtoParser(storage, DefaultObjectMapper.get());
        bitbucketServerAggHelper = new BitbucketServerAggHelper(parser, scmAggService);

        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    public Map<String, Object> createMockStorageResult() {
        String PULL_REQUEST_DATATYPE = "pull_requests";

        var s = StorageResult.builder()
                .storageMetadata(StorageMetadata.builder()
                        .dataType(PULL_REQUEST_DATATYPE)
                        .key(IntegrationKey.builder()
                                .integrationId("1")
                                .tenantId("test")
                                .build())
                        .integrationType("bitbucket_server").build())
                .record(GcsDataResult.builder()
                        .blobId(BlobId.builder()
                                .bucket("test")
                                .generation(1L)
                                .name("test").build())
                        .uri("test_uri")
                        .htmlUri("test_html_uri")
                        .build())
                .build();
        var mapper = DefaultObjectMapper.get();
        var wrapped = ListResponse.builder().record(s).build();
        return mapper.convertValue(wrapped, Map.class);
    }

    private void setupGoogleStorageMock(String response) throws UnsupportedEncodingException {
        when(storage.readAllBytes(any())).thenReturn(response.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void insertBitbucketServerPrsTest() throws IOException, SQLException {
        MultipleTriggerResults results = MultipleTriggerResults.builder()
                .triggerResults(List.of(TriggerResults.builder()
                        .jobs(List.of(JobDTO.builder()
                                .result(createMockStorageResult()).build()))
                        .build()))
                .build();
        // This file contains data for 1 PR and 1 associated commit
        String pull_request_content = ResourceUtils.getResourceAsString("bitbucket-server/sample_pullrequest_with_commit.json");
        setupGoogleStorageMock(pull_request_content);
        bitbucketServerAggHelper.insertBitbucketServerPrs(company, "1", results, new Date());
        var prListResponse = scmAggService.list(company, 0, 1);
        var commitListResponse = scmAggService.listCommits(
                company, ScmCommitFilter.builder().build(),Map.of(), OUConfiguration.builder().build(), 0, 1);
        var pr = prListResponse.getRecords().get(0);
        var commit = commitListResponse.getRecords().get(0);

        assertThat(prListResponse.getCount()).isEqualTo(1);
        assertThat(commitListResponse.getCount()).isEqualTo(1);
        assertThat(pr.getCommitShas()).isEqualTo(List.of("d33d4618cf3e55933d92a44087e708e1d52a6d24"));
        assertThat(commit.getCommitSha()).isEqualTo("d33d4618cf3e55933d92a44087e708e1d52a6d24");
        assertThat(commit.getAuthor()).isEqualTo(pr.getCreator());
    }
}
