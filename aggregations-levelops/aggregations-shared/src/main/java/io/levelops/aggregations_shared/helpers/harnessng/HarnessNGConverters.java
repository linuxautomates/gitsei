package io.levelops.aggregations_shared.helpers.harnessng;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.ArtifactDockerPush.StepArtifacts.PublishedImageArtifact;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep.Outcomes.DeploymentInfoOutcome.ServerInstanceInfo.Container;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class HarnessNGConverters {

    private static final Set<String> ROLLBACK_NODE_IDENTIFIERS = Stream.of("rollbackRolloutDeployment", "rollbackSteps")
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    private static final Pattern DOCKER_IMAGE_PATTERN = Pattern.compile("((.*)/)?([^/:]*)(:(.*))?"); // location/image-name:version

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<CiCdJobRunArtifact> parseArtifacts(CICDJobRun ciCdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        return Stream.of(
                        parseDockerContainerPush(ciCdJobRun, executionGraph),
                        parsePublishedImageArtifactsGeneric(ciCdJobRun, executionGraph),
                        parseDeploymentInfoOutcome(ciCdJobRun, executionGraph),
                        parseArtifactsOutcome(ciCdJobRun, executionGraph)
                )
                .flatMap(Function.identity())
                .distinct()
                .collect(Collectors.toList());
    }

    private static Stream<CiCdJobRunArtifact> parseDockerContainerPush(CICDJobRun ciCdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        // executionGraph -> nodeMap -> * -> outcomes -> artifact_Docker_Push -> stepArtifacts -> publishedImageArtifacts []
        return MapUtils.emptyIfNull(executionGraph.getNodeMap()).values().stream()
                .map(HarnessNGPipelineStageStep::getOutcomes)
                .filter(Objects::nonNull)
                .map(Outcomes::getArtifactDockerPush)
                .filter(Objects::nonNull)
                .map(ArtifactDockerPush::getStepArtifacts)
                .filter(Objects::nonNull)
                .map(StepArtifacts::getPublishedImageArtifacts)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(artifact -> parsePublishedImageArtifact(ciCdJobRun, artifact));
    }

    /**
     * Parses "published image artifacts" outcomes from any stage
     * (except the ones explicitly called out in the model like artifact_Docker_Push)
     */
    private static Stream<CiCdJobRunArtifact> parsePublishedImageArtifactsGeneric(CICDJobRun ciCdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        // executionGraph -> nodeMap -> * -> outcomes -> * -> stepArtifacts -> publishedImageArtifacts []
        return MapUtils.emptyIfNull(executionGraph.getNodeMap()).values().stream()
                .map(HarnessNGPipelineStageStep::getOutcomes)
                .filter(Objects::nonNull)
                .flatMap(outcomes -> MapUtils.emptyIfNull(outcomes.getDynamicFields()).values().stream())
                .filter(outcomeObject -> outcomeObject instanceof Map<?, ?>)
                .map(outcomeObject -> {
                    try {
                        return DefaultObjectMapper.get().convertValue(outcomeObject, HarnessNGGenericCiStageOutcome.class);
                    } catch (IllegalArgumentException e) {
                        // ignore
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(HarnessNGGenericCiStageOutcome::getStepArtifacts)
                .filter(Objects::nonNull)
                .map(StepArtifacts::getPublishedImageArtifacts)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(artifact -> parsePublishedImageArtifact(ciCdJobRun, artifact));
    }

    private static CiCdJobRunArtifact parsePublishedImageArtifact(CICDJobRun ciCdJobRun, PublishedImageArtifact apiArtifact) {
        return CiCdJobRunArtifact.builder()
                .cicdJobRunId(ciCdJobRun.getId())
                .input(ciCdJobRun.getCi())
                .output(ciCdJobRun.getCd())
                .type(CiCdJobRunArtifact.CONTAINER_TYPE)
                .location(StringUtils.defaultString(apiArtifact.getUrl()))
                .name(StringUtils.defaultString(apiArtifact.getImageName()))
                .qualifier(StringUtils.defaultString(apiArtifact.getTag()))
                .hash(StringUtils.defaultString(apiArtifact.getDigest()))
                .metadata(Map.of())
                .build();
    }

    private static Stream<CiCdJobRunArtifact> parseDeploymentInfoOutcome(CICDJobRun ciCdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        // executionGraph -> nodeMap -> * -> outcomes -> deploymentInfoOutcome -> serverInstanceInfoList [] -> containerList []
        return MapUtils.emptyIfNull(executionGraph.getNodeMap()).values().stream()
                .map(HarnessNGPipelineStageStep::getOutcomes)
                .filter(Objects::nonNull)
                .map(Outcomes::getDeploymentInfoOutcome)
                .filter(Objects::nonNull)
                .map(Outcomes.DeploymentInfoOutcome::getServerInstanceInfoList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(Outcomes.DeploymentInfoOutcome.ServerInstanceInfo::getContainerList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(artifact -> parseDeploymentInfoContainer(ciCdJobRun, artifact));
    }

    private static Stream<CiCdJobRunArtifact> parseArtifactsOutcome(CICDJobRun ciCdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        // executionGraph -> nodeMap -> * -> outcomes -> artifacts -> primary
        return MapUtils.emptyIfNull(executionGraph.getNodeMap()).values().stream()
                .map(HarnessNGPipelineStageStep::getOutcomes)
                .filter(Objects::nonNull)
                .filter(outcomes -> MapUtils.emptyIfNull(outcomes.getDynamicFields()).containsKey("artifacts"))
                .map(outcomeObject -> {
                    try {
                        return DefaultObjectMapper.get().convertValue(outcomeObject.getDynamicFields(), HarnessNGArtifactsOutcome.class);
                    } catch (IllegalArgumentException e) {
                        // ignore
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(HarnessNGArtifactsOutcome::getArtifacts)
                .filter(Objects::nonNull)
                .map(HarnessNGArtifactsOutcome.Artifacts::getPrimary)
                .filter(Objects::nonNull)
                .map(primary -> parseGenericContainer(ciCdJobRun, primary.getImage(), null));
               
    }

    private static CiCdJobRunArtifact parseDeploymentInfoContainer(CICDJobRun ciCdJobRun, Container apiArtifact) {
        return parseGenericContainer(ciCdJobRun, apiArtifact.getImage(), apiArtifact.getName());
    }

    private static CiCdJobRunArtifact parseGenericContainer(@Nonnull CICDJobRun ciCdJobRun, @Nullable String image, @Nullable String containerName) {
        String location = "";
        String name = "";
        String qualifier = "";
        Matcher matcher = StringUtils.isNotBlank(image)
                ? DOCKER_IMAGE_PATTERN.matcher(image)
                : null;
        if (matcher != null && matcher.matches()) {
            location = matcher.group(2);
            name = matcher.group(3);
            qualifier = matcher.group(5);
        }
        name = StringUtils.firstNonBlank(name, containerName, image, "");
        return CiCdJobRunArtifact.builder()
                .cicdJobRunId(ciCdJobRun.getId())
                .input(ciCdJobRun.getCi())
                .output(ciCdJobRun.getCd())
                .type(CiCdJobRunArtifact.CONTAINER_TYPE)
                .location(StringUtils.defaultString(location))
                .name(StringUtils.defaultString(name))
                .qualifier(StringUtils.defaultString(qualifier))
                .hash("")
                .metadata(Map.of())
                .build();
    }

    public static Map<String, Object> parseExecutionMetadataFields(HarnessNGPipelineExecution execution) {
        Map<String, Object> metadata = new HashMap<>();

        HarnessNGPipeline pipeline = execution.getPipeline();
        if (pipeline != null) {
            Optional.ofNullable(pipeline.getModuleInfo()).map(HarnessNGPipeline.ModuleInfo::getCdModule).ifPresent(cdModule -> {
                addValuesToMetadata(metadata, "env_ids", cdModule.getEnvIdentifiers());
                addValuesToMetadata(metadata, "env_types", cdModule.getEnvironmentTypes());
                addValuesToMetadata(metadata, "env_groups", cdModule.getEnvGroupIdentifiers());
                addValuesToMetadata(metadata, "service_ids", cdModule.getServiceIdentifiers());
                addValuesToMetadata(metadata, "service_types", cdModule.getServiceDefinitionTypes());
                addValuesToMetadata(metadata, "infra_ids", cdModule.getInfrastructureIdentifiers());
                addValuesToMetadata(metadata, "infra_names", cdModule.getInfrastructureNames());
                addValuesToMetadata(metadata, "infra_types", cdModule.getInfrastructureTypes());
            });

            Optional.ofNullable(pipeline.getModuleInfo()).map(HarnessNGPipeline.ModuleInfo::getCiModule).ifPresent(ciModule -> {
                metadata.put("branch", StringUtils.defaultString(ciModule.getBranch()));
                metadata.put("repo_name", StringUtils.defaultString(ciModule.getRepoName()));
                metadata.put("repo_url", IterableUtils.getFirst(ciModule.getScmDetailsList())
                        .map(HarnessNGPipeline.ScmDetails::getScmUrl)
                        .filter(StringUtils::isNotBlank)
                        .map(String::toLowerCase)
                        .orElse(""));
            });

            metadata.put("tags", ListUtils.stream(pipeline.getTags())
                    .filter(Objects::nonNull)
                    .map(HarnessNGPipeline.Tag::getKey)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList()));
        }

        metadata.put("rollback", isRollback(execution.getExecutionGraph()));

        return metadata;
    }

    private static void addValuesToMetadata(Map<String, Object> metadata, String key, List<String> values) {
        metadata.put(key, ListUtils.stream(values)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public static boolean isRollback(HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        if (executionGraph == null) {
            return false;
        }
        return MapUtils.emptyIfNull(executionGraph.getNodeMap()).values().stream()
                .anyMatch(node -> ROLLBACK_NODE_IDENTIFIERS.contains(StringUtils.defaultString(node.getIdentifier()).toLowerCase()));
    }

    public static List<CICDJobRun.JobRunParam> parseExecutionParams(HarnessNGPipelineExecution execution) {
        if (execution.getInputSet() == null || StringUtils.isEmpty(execution.getInputSet().getInputSetYaml())) {
            return Collections.emptyList();
        }
        String inputSetYamlString = execution.getInputSet().getInputSetYaml();
        HarnessNGInputSetYaml inputSetYaml = null;
        try {
            inputSetYaml = YAML_MAPPER.readValue(inputSetYamlString, HarnessNGInputSetYaml.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse input set for execution id={}", execution.getPipeline().getExecutionId(), e);
            return Collections.emptyList();
        }

        List<HarnessNGInputSetYaml.Pipeline.Variable> variables = Optional.ofNullable(inputSetYaml)
                .map(HarnessNGInputSetYaml::getPipeline)
                .map(HarnessNGInputSetYaml.Pipeline::getVariables)
                .orElse(Collections.emptyList());
        return ListUtils.stream(variables)
                .map(variable -> CICDJobRun.JobRunParam.builder()
                        .name(variable.getName())
                        .type(variable.getType())
                        .value(String.valueOf(variable.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

}
