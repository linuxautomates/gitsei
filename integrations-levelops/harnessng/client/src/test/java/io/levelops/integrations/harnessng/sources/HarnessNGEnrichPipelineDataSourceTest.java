package io.levelops.integrations.harnessng.sources;


import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.harnessng.client.HarnessNGClient;
import io.levelops.integrations.harnessng.client.HarnessNGClientException;
import io.levelops.integrations.harnessng.client.HarnessNGClientFactory;
import io.levelops.integrations.harnessng.client.HarnessNGClientResilienceTest;
import io.levelops.integrations.harnessng.models.HarnessNGExecutionInputSet;
import io.levelops.integrations.harnessng.models.HarnessNGIngestionQuery;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import io.levelops.integrations.harnessng.source.HarnessNGEnrichPipelineDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HarnessNGEnrichPipelineDataSourceTest {

    private HarnessNGClient client;
    private static final String ORG = "org1";
    private static final String PROJECT = "proj1";
    private static final String ORG_PROJECT = ORG + "/" + PROJECT;

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();

    HarnessNGEnrichPipelineDataSource dataSource;

    @Before
    public void setup() throws HarnessNGClientException {
        client = Mockito.mock(HarnessNGClient.class);
        HarnessNGClientFactory clientFactory = Mockito.mock(HarnessNGClientFactory.class);
        dataSource = new HarnessNGEnrichPipelineDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(HarnessNGIngestionQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<HarnessNGPipelineExecution>> pipelineData = dataSource.fetchMany(HarnessNGIngestionQuery.builder()
                        .integrationKey(TEST_KEY)
                        .accountIdentifier(HarnessNGClientResilienceTest.accountIdentifier)
                        .organizations(List.of())
                        .from(null)
                        .build())
                .collect(Collectors.toList());
        assertThat(pipelineData).hasSize(0);
    }

    @Test
    public void fetchManyWithNullOrgProjectValue() throws FetchException {
        Date from = Date.from(Instant.ofEpochMilli(1675209600000L));
        Date to = Date.from(Instant.ofEpochMilli(1675728000000L));
        HarnessNGPipelineExecution execution = HarnessNGPipelineExecution.builder()
                .pipeline(HarnessNGPipeline.builder()
                        .executionId("exec1")
                        .startTs(1675555200000L)
                        .endTs(1675555200000L)
                        .build())
                .build();
        HarnessNGExecutionInputSet inputSet = HarnessNGExecutionInputSet.builder()
                .inputSetYaml("yaml")
                .inputSetTemplateYaml("template")
                .build();
        when(client.streamProjects(anyString())).thenReturn(Stream.of(HarnessNGProject.builder().project(HarnessNGProject.Project.builder().identifier(PROJECT).orgIdentifier(ORG).build()).build()));
        when(client.streamExecutions(eq(HarnessNGClientResilienceTest.accountIdentifier), eq(PROJECT), eq(ORG), any(), any())).thenReturn(Stream.of(execution.getPipeline()));
        when(client.getPipelineExecutionDetails(eq(HarnessNGClientResilienceTest.accountIdentifier), eq(PROJECT), eq(ORG), eq("exec1"), anyBoolean())).thenReturn(execution);
        when(client.getExecutionInputSet(eq(HarnessNGClientResilienceTest.accountIdentifier), eq(PROJECT), eq(ORG), eq("exec1"), anyBoolean())).thenReturn(inputSet);


        List<Data<HarnessNGPipelineExecution>> pipelineData = dataSource.fetchMany(HarnessNGIngestionQuery.builder()
                        .integrationKey(TEST_KEY)
                        .accountIdentifier(HarnessNGClientResilienceTest.accountIdentifier)
                        .organizations(List.of())
                        .projects(List.of(ORG_PROJECT))
                        .from(from)
                        .to(to)
                        .build())
                .collect(Collectors.toList());
        assertThat(pipelineData).hasSize(1);
        HarnessNGPipelineExecution output = pipelineData.get(0).getPayload();

        assertThat(pipelineData.get(0).getPayload().getPipeline()).isEqualTo(output.getPipeline().toBuilder()
                .projectIdentifier(PROJECT)
                .orgIdentifier(ORG)
                .build());
        assertThat(output.getInputSet()).isEqualTo(inputSet);
    }
}