package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts.PublishedImageArtifact;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.DeploymentInfoOutcome.ServerInstanceInfo.Container;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HarnessNGPipelineStageStepTest {

    @Test
    public void deserializeDeploymentInfoOutcome() throws IOException {

        HarnessNGPipelineStageStep resource = ResourceUtils.getResourceAsObject("integrations/harnessng/node-with-cd-outcome.json", HarnessNGPipelineStageStep.class);
        String json = DefaultObjectMapper.writeAsPrettyJson(resource);
        System.out.println(json);
        assertThat(json).contains("----test-field");

        assertThat(resource.getOutcomes()).isNotNull();
        assertThat(resource.getOutcomes().getDeploymentInfoOutcome()).isNotNull();
        assertThat(resource.getOutcomes().getDeploymentInfoOutcome().getServerInstanceInfoList()).hasSize(1);
        assertThat(resource.getOutcomes().getDeploymentInfoOutcome().getServerInstanceInfoList().get(0).getContainerList()).hasSize(1);

        Container container = resource.getOutcomes().getDeploymentInfoOutcome().getServerInstanceInfoList().get(0).getContainerList().get(0);
        assertThat(container.getContainerId()).isEqualTo("containerd://8121d9d18c181b52aa1b036c1efddedde43c803b14c3803bda47425fd87e730f");
        assertThat(container.getName()).isEqualTo("harness-example");
        assertThat(container.getImage()).isEqualTo("docker.io/meetrajsinhcrest/harness-test:0.0.2");
        assertThat(container.getDynamicFields()).isNotEmpty();
        assertThat(container.getDynamicFields()).containsEntry("----test-field", "abc");

    }

    @Test
    public void deserializeArtifactDockerPush() throws IOException {

        HarnessNGPipelineStageStep resource = ResourceUtils.getResourceAsObject("integrations/harnessng/node-with-ci-outcome.json", HarnessNGPipelineStageStep.class);
        String json = DefaultObjectMapper.writeAsPrettyJson(resource);
        System.out.println(json);
        assertThat(json).contains("----test-field");

        assertThat(resource.getOutcomes()).isNotNull();
        assertThat(resource.getOutcomes().getArtifactDockerPush()).isNotNull();
        assertThat(resource.getOutcomes().getArtifactDockerPush().getStepArtifacts()).isNotNull();
        assertThat(resource.getOutcomes().getArtifactDockerPush().getStepArtifacts().getPublishedImageArtifacts()).hasSize(1);

        PublishedImageArtifact artifact = resource.getOutcomes().getArtifactDockerPush().getStepArtifacts().getPublishedImageArtifacts().get(0);
        assertThat(artifact.getImageName()).isEqualTo("image1");
        assertThat(artifact.getTag()).isEqualTo("v1");
        assertThat(artifact.getUrl()).isEqualTo("location");
        assertThat(artifact.getDynamicFields()).isNotEmpty();
        assertThat(artifact.getDynamicFields()).containsEntry("----test-field", "abc");

    }

    @Test
    public void deserializeDynamicOutcome() throws JsonProcessingException {
        String json = "{\"outcomes\": { \"----test-field\": {\"a\":\"b\"} } }";
        HarnessNGPipelineStageStep o = DefaultObjectMapper.get().readValue(json, HarnessNGPipelineStageStep.class);

        assertThat(o.getOutcomes().getDynamicFields()).hasSize(1);
        assertThat(o.getOutcomes().getDynamicFields()).containsEntry("----test-field", Map.of("a", "b"));
    }
}