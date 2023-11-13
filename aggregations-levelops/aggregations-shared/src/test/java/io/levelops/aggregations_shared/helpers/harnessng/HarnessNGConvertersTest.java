package io.levelops.aggregations_shared.helpers.harnessng;

import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRun.JobRunParam;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.aggregations_shared.helpers.harnessng.HarnessNGConverters.parseExecutionMetadataFields;
import static org.assertj.core.api.Assertions.assertThat;

public class HarnessNGConvertersTest {

    @Test
    public void parseDockerContainerPush() throws IOException {
        HarnessNGPipelineExecution.ExecutionGraph input = ResourceUtils.getResourceAsObject("harnessng/execution_graph_with_docker_push.json", HarnessNGPipelineExecution.ExecutionGraph.class);

        UUID cicdJobRunID = UUID.randomUUID();
        List<CiCdJobRunArtifact> output = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(true)
                        .cd(false)
                        .build(),
                input);

        assertThat(output).containsExactlyInAnyOrder(
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("location")
                        .name("image1")
                        .qualifier("v1")
                        .hash("h1")
                        .metadata(Map.of())
                        .build());

        List<CiCdJobRunArtifact> output2 = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(false)
                        .cd(true)
                        .build(),
                input);

        assertThat(output2).containsExactlyInAnyOrder(
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(false)
                        .output(true)
                        .type("container")
                        .location("location")
                        .name("image1")
                        .qualifier("v1")
                        .hash("h1")
                        .metadata(Map.of())
                        .build());
    }

    @Test
    public void parsePublishedImageArtifactsGeneric() throws IOException {
        HarnessNGPipelineExecution.ExecutionGraph input = ResourceUtils.getResourceAsObject("harnessng/execution_graph_publishedImageArtifactsGeneric.json", HarnessNGPipelineExecution.ExecutionGraph.class);

        UUID cicdJobRunID = UUID.randomUUID();
        List<CiCdJobRunArtifact> output = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(true)
                        .cd(false)
                        .build(),
                input);

        assertThat(output).containsExactlyInAnyOrder(
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("location")
                        .name("image1")
                        .qualifier("v1")
                        .hash("h1")
                        .metadata(Map.of())
                        .build(),
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("location")
                        .name("image2")
                        .qualifier("v2")
                        .hash("h2")
                        .metadata(Map.of())
                        .build());

    }

    @Test
    public void parseArtifactsOutcome() throws IOException {
        HarnessNGPipelineExecution.ExecutionGraph input = ResourceUtils.getResourceAsObject("harnessng/execution_graph_with_artifacts_outcome.json", HarnessNGPipelineExecution.ExecutionGraph.class);

        UUID cicdJobRunID = UUID.randomUUID();
        List<CiCdJobRunArtifact> output = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(true)
                        .cd(false)
                        .build(),
                input);

        assertThat(output).containsExactlyInAnyOrder(
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("index.docker.io/juhiagr")
                        .name("sei-repo")
                        .qualifier("2.0")
                        .hash("")
                        .metadata(Map.of())
                        .build());

    }

    @Test
    public void parseDeploymentInfoContainer() throws IOException {
        HarnessNGPipelineExecution.ExecutionGraph input = ResourceUtils.getResourceAsObject("harnessng/execution_graph_with_deployment_info.json", HarnessNGPipelineExecution.ExecutionGraph.class);

        UUID cicdJobRunID = UUID.randomUUID();
        List<CiCdJobRunArtifact> output = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(true)
                        .cd(false)
                        .build(),
                input);

        assertThat(output).containsExactlyInAnyOrder(
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("docker.io/meetrajsinhcrest")
                        .name("harness-test")
                        .qualifier("0.0.2")
                        .hash("")
                        .metadata(Map.of())
                        .build(),
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("")
                        .name("harness-test2")
                        .qualifier("0.0.3")
                        .hash("")
                        .metadata(Map.of())
                        .build(),
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("docker.io/meetrajsinhcrest")
                        .name("harness-test3")
                        .qualifier("")
                        .hash("")
                        .metadata(Map.of())
                        .build(),
                CiCdJobRunArtifact.builder()
                        .cicdJobRunId(cicdJobRunID)
                        .input(true)
                        .output(false)
                        .type("container")
                        .location("")
                        .name("harness-example")
                        .qualifier("")
                        .hash("")
                        .metadata(Map.of())
                        .build());

        List<CiCdJobRunArtifact> output2 = HarnessNGConverters.parseArtifacts(
                CICDJobRun.builder()
                        .id(cicdJobRunID)
                        .ci(false)
                        .cd(true)
                        .build(),
                input);
        assertThat(output2.stream().map(CiCdJobRunArtifact::getInput)).containsOnly(false);
        assertThat(output2.stream().map(CiCdJobRunArtifact::getOutput)).containsOnly(true);
    }


    @Test
    public void testMetadata() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/harnessng_pipeline_execution_with_cd.json", HarnessNGPipelineExecution.class);

        Map<String, Object> metadata = parseExecutionMetadataFields(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(metadata);

        Map<?, ?> expected = ResourceUtils.getResourceAsObject("harnessng/harnessng_pipeline_execution_with_cd__expected_metadata.json", Map.class);
        assertThat(metadata).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void testMetadataWithTags() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_tags.json", HarnessNGPipelineExecution.class);

        Map<String, Object> metadata = parseExecutionMetadataFields(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(metadata);

        Map<?, ?> expected = ResourceUtils.getResourceAsObject("harnessng/execution_with_tags__expected_metadata.json", Map.class);
        assertThat(metadata).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void testMetadataWithRollback() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_rollback.json", HarnessNGPipelineExecution.class);

        Map<String, Object> metadata = parseExecutionMetadataFields(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(metadata);

        Map<?, ?> expected = ResourceUtils.getResourceAsObject("harnessng/execution_with_rollback__expected_metadata.json", Map.class);
        assertThat(metadata).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void testMetadataWithCI() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_ci.json", HarnessNGPipelineExecution.class);

        Map<String, Object> metadata = parseExecutionMetadataFields(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(metadata);

        Map<?, ?> expected = ResourceUtils.getResourceAsObject("harnessng/execution_with_ci__expected_metadata.json", Map.class);
        assertThat(metadata).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void parseExecutionParams() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_input_set.json", HarnessNGPipelineExecution.class);

        List<JobRunParam> output = HarnessNGConverters.parseExecutionParams(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(output);

        List<JobRunParam> expected = ResourceUtils.getResourceAsObject("harnessng/execution_with_input_set__expected_input_set.json",
                DefaultObjectMapper.get().getTypeFactory().constructCollectionType(List.class, JobRunParam.class));
        assertThat(output).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void parseExecutionParams2() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_input_set_2.json", HarnessNGPipelineExecution.class);

        List<JobRunParam> output = HarnessNGConverters.parseExecutionParams(pipelineExecutionRecord);
        DefaultObjectMapper.prettyPrint(output);

        // Not supported yet, so it returns empty - but we could change this in the future
        assertThat(output).isEmpty();

    }

}