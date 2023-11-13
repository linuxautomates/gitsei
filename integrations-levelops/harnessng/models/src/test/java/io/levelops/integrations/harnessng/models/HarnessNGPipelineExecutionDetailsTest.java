package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HarnessNGPipelineExecutionDetailsTest {

    @Test
    public void testDeserialize() throws IOException {

        ObjectMapper mapper = DefaultObjectMapper.get();
        HarnessNGAPIResponse<HarnessNGPipelineExecution> response = mapper.readValue(
                ResourceUtils.getResourceAsString("harnessng/harnessng_api_pipeline_execution_details.json"),
                mapper.constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGPipelineExecution>>() {}));

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getData());

        HarnessNGPipelineExecution pipelineExecution = HarnessNGPipelineExecution.builder()
                .pipeline(HarnessNGPipeline.builder()
                        .identifier("Messy_Pipeline")
                        .executionId("bEdpBLFcR5iVx6Up4C9uDw")
                        .name("Messy Pipeline")
                        .status("Failed")
                        .runSequence(3L)
                        .executionTriggerInfo(HarnessNGPipeline.ExecutionTriggerInfo.builder()
                                .triggeredByUser(HarnessNGPipeline.ExecutionTriggerInfo.TriggeredUser.builder()
                                        .uuid("q27LHq2XSSam3H09Rrqevg")
                                        .identifier("Meetrajsinh Solanki")
                                        .extraInfo(Map.of("email", "meetrajsinh.solanki@crestdatasys.com"))
                                        .build())
                                .build())
                        .startTs(1672649303018L)
                        .endTs(1672649305628L)
                        .createdAt(1672649303214L)
                        .build())
                .executionGraph(HarnessNGPipelineExecution.ExecutionGraph.builder()
                        .rootNodeId("tyOGymiSTS2LzxMplksxDQ")
                        .nodeMap(Map.of(
                                "tyOGymiSTS2LzxMplksxDQ",
                                HarnessNGPipelineStageStep.builder()
                                        .uuid("tyOGymiSTS2LzxMplksxDQ")
                                        .name("Messy Pipeline")
                                        .identifier("pipeline")
                                        .baseFqn("pipeline")
                                        .stageType("PIPELINE_SECTION")
                                        .status("Failed")
                                        .startTs(1672649303246L)
                                        .endTs(1672649305628L)
                                        .outcomes(HarnessNGPipelineStageStep.Outcomes.builder()
                                                .artifactDockerPush(HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.builder()
                                                        .stepArtifacts(HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts.builder()
                                                                .publishedImageArtifacts(
                                                                        List.of(
                                                                                HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts.PublishedImageArtifact.builder()
                                                                                        .digest("sha256:9399b7edc64bfddab1d4524b4cf10378e803b4f16d144b902141e0caf1e75c3e")
                                                                                        .imageName("harness/nextgenui")
                                                                                        .tag("cds-49176-ui")
                                                                                        .url("https://hub.docker.com/layers/harness/nextgenui/cds-49176-ui/images/sha256-9399b7edc64bfddab1d4524b4cf10378e803b4f16d144b902141e0caf1e75c3e")
                                                                                        .build()
                                                                        )
                                                                )
                                                                .build())
                                                        .build())
                                                .build())
                                        .build(),
                                "DZQUPR5vSyO1TW0zDRuz_g",
                                HarnessNGPipelineStageStep.builder()
                                        .uuid("DZQUPR5vSyO1TW0zDRuz_g")
                                        .name("stages")
                                        .identifier("stages")
                                        .baseFqn("pipeline.stages")
                                        .stageType("STAGES_STEP")
                                        .status("Failed")
                                        .startTs(1672649303280L)
                                        .endTs(1672649305596L)
                                        .build(),
                                "GrKA0klDRzu0lAL1UeQcTA",
                                HarnessNGPipelineStageStep.builder()
                                        .uuid("GrKA0klDRzu0lAL1UeQcTA")
                                        .name("Custom1")
                                        .identifier("Custom1")
                                        .baseFqn("pipeline.stages.Custom1")
                                        .stageType("CUSTOM_STAGE")
                                        .status("Failed")
                                        .startTs(1672649303351L)
                                        .endTs(1672649305118L)
                                        .build()))
                        .nodeAdjacencyListMap(Map.of(
                                "tyOGymiSTS2LzxMplksxDQ", HarnessNGPipelineExecution.ExecutionGraph.NodeAdjacency.builder()
                                        .children(List.of("DZQUPR5vSyO1TW0zDRuz_g"))
                                        .nextIds(List.of())
                                        .build(),
                                "DZQUPR5vSyO1TW0zDRuz_g", HarnessNGPipelineExecution.ExecutionGraph.NodeAdjacency.builder()
                                        .children(List.of("GrKA0klDRzu0lAL1UeQcTA"))
                                        .nextIds(List.of())
                                        .build(),
                                "GrKA0klDRzu0lAL1UeQcTA", HarnessNGPipelineExecution.ExecutionGraph.NodeAdjacency.builder()
                                        .children(List.of())
                                        .nextIds(List.of())
                                        .build()))
                        .build())
                .build();
        Assert.assertEquals(response.getData().getPipeline().getIdentifier(), pipelineExecution.getPipeline().getIdentifier());
        Assert.assertEquals(response.getData().getPipeline().getStatus(), pipelineExecution.getPipeline().getStatus());
        Assert.assertEquals(response.getData().getPipeline().getRunSequence(), pipelineExecution.getPipeline().getRunSequence());
        Assert.assertEquals(response.getData().getExecutionGraph().getNodeMap().get("tyOGymiSTS2LzxMplksxDQ").getOutcomes(), pipelineExecution.getExecutionGraph().getNodeMap().get("tyOGymiSTS2LzxMplksxDQ").getOutcomes());
    }
}
