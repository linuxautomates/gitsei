package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGPipeline.HarnessNGPipelineBuilder.class)
public class HarnessNGPipeline {

    @JsonProperty("accountIdentifier")
    String accountIdentifier;

    @JsonProperty("orgIdentifier")
    String orgIdentifier;

    @JsonProperty("projectIdentifier")
    String projectIdentifier;

    @JsonProperty("pipelineIdentifier")
    String identifier;

    @JsonProperty("planExecutionId")
    String executionId;

    @JsonProperty("name")
    String name;

    @JsonProperty("status")
    String status;

    @JsonProperty("runSequence")
    Long runSequence;

    @JsonProperty("executionTriggerInfo")
    ExecutionTriggerInfo executionTriggerInfo;

    @JsonProperty("startTs")
    Long startTs;

    @JsonProperty("endTs")
    Long endTs;

    @JsonProperty("createdAt")
    Long createdAt;

    @JsonProperty("gitDetails")
    GitDetails gitDetails;

    @JsonProperty("layoutNodeMap")
    Map<String, Node> layoutNodeMap;

    @JsonProperty("moduleInfo")
    ModuleInfo moduleInfo;

    @JsonProperty("tags")
    List<Tag> tags;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Tag.TagBuilder.class)
    public static class Tag {
        @JsonProperty("key")
        String key;
        @JsonProperty("value")
        String value;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Node.NodeBuilder.class)
    public static class Node {
        @JsonProperty("nodeType")
        String nodeType;
        @JsonProperty("nodeGroup")
        String nodeGroup;
        @JsonProperty("nodeIdentifier")
        String nodeIdentifier;
        @JsonProperty("name")
        String name;
        @JsonProperty("nodeUuid")
        String nodeUuid;
        @JsonProperty("status")
        String status;
        @JsonProperty("module")
        String module;
        @JsonProperty("moduleInfo")
        ModuleInfo moduleInfo;
        @JsonProperty("startTs")
        Long startTs;
        @JsonProperty("endTs")
        Long endTs;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ExecutionTriggerInfo.ExecutionTriggerInfoBuilder.class)
    public static class ExecutionTriggerInfo {

        @JsonProperty("triggeredBy")
        ExecutionTriggerInfo.TriggeredUser triggeredByUser;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = ExecutionTriggerInfo.TriggeredUser.TriggeredUserBuilder.class)
        public static class TriggeredUser {

            @JsonProperty("uuid")
            String uuid;

            @JsonProperty("identifier")
            String identifier;

            @JsonProperty("extraInfo")
            Map<String, Object> extraInfo;

            public String getEmailFromExtraInfo() {
                return ObjectUtils.defaultIfNull(MapUtils.emptyIfNull(extraInfo).get("email"), "").toString();
            }

        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ServiceInfo.ServiceInfoBuilder.class)
    public static class ServiceInfo {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("identifier")
        String identifier;
        @JsonProperty("displayName")
        String displayName;
        @JsonProperty("deploymentType")
        String deploymentType;
        @JsonProperty("gitOpsEnabled")
        Boolean gitOpsEnabled;
        @JsonProperty("artifacts")
        Artifacts artifacts;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = HarnessNGPipeline.ServiceInfo.Artifacts.ArtifactsBuilder.class)
        public static class Artifacts {
            @JsonProperty("__recast")
            String recast;

            @JsonProperty("primary")
            ImageSummary primary;

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = HarnessNGPipeline.ServiceInfo.Artifacts.ImageSummary.ImageSummaryBuilder.class)
            public static class ImageSummary {
                @JsonProperty("__recast")
                String recast;
                @JsonProperty("imagePath")
                String imagePath;
                @JsonProperty("tag")
                String tag;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = InfraExecutionSummary.InfraExecutionSummaryBuilder.class)
    public static class InfraExecutionSummary {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("identifier")
        String identifier;
        @JsonProperty("name")
        String name;
        @JsonProperty("type")
        String type;
        @JsonProperty("infrastructureIdentifier")
        String infrastructureIdentifier;
        @JsonProperty("infrastructureName")
        String infrastructureName;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ModuleInfo.ModuleInfoBuilder.class)
    public static class ModuleInfo {
        @JsonProperty("ci")
        CIModuleInfo ciModule;

        @JsonProperty("cd")
        CDModuleInfo cdModule;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = HarnessNGPipeline.ModuleInfo.CIModuleInfo.CIModuleInfoBuilder.class)
        public static class CIModuleInfo {
            @JsonProperty("__recast")
            String recast;
            @JsonProperty("branch")
            String branch;
            @JsonProperty("buildType")
            String buildType;
            @JsonProperty("repoName")
            String repoName;
            @JsonProperty("ciExecutionInfoDTO")
            ExecutionInfoDTO ExecutionInfoDTO;
            @JsonProperty("imageDetailsList")
            List<ImageDetails> imageDetailsList;
            @JsonProperty("infraDetailsList")
            List<InfraDetails> infraDetailsList;
            @JsonProperty("scmDetailsList")
            List<ScmDetails> scmDetailsList;
            @JsonProperty("ciPipelineStageModuleInfo")
            PipelineStageModuleInfo ciPipelineStageModuleInfo;
        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = HarnessNGPipeline.ModuleInfo.CDModuleInfo.CDModuleInfoBuilder.class)
        public static class CDModuleInfo {
            @JsonProperty("__recast")
            String recast;

            @JsonProperty("envGroupIdentifiers")
            List<String> envGroupIdentifiers;

            @JsonProperty("envIdentifiers")
            List<String> envIdentifiers;

            @JsonProperty("environmentTypes")
            List<String> environmentTypes;

            @JsonProperty("freezeIdentifiers")
            List<String> freezeIdentifiers;

            @JsonProperty("infrastructureIdentifiers")
            List<String> infrastructureIdentifiers;

            @JsonProperty("infrastructureNames")
            List<String> infrastructureNames;

            @JsonProperty("infrastructureTypes")
            List<String> infrastructureTypes;

            @JsonProperty("serviceDefinitionTypes")
            List<String> serviceDefinitionTypes;

            @JsonProperty("serviceIdentifiers")
            List<String> serviceIdentifiers;

            @JsonProperty("serviceInfo")
            ServiceInfo serviceInfo;

            @JsonProperty("infraExecutionSummary")
            InfraExecutionSummary infraExecutionSummary;

        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = HarnessNGPipeline.ModuleInfo.PipelineStageModuleInfo.PipelineStageModuleInfoBuilder.class)
        public static class PipelineStageModuleInfo {
            @JsonProperty("__recast")
            String recast;
            @JsonProperty("stageExecutionId")
            String stageExecutionId;
            @JsonProperty("stageId")
            String stageId;
            @JsonProperty("stageName")
            String stageName;
            @JsonProperty("cpuTime")
            Long cpuTime;
            @JsonProperty("stageBuildTime")
            Long stageBuildTime;
            @JsonProperty("infraType")
            String infraType;
            @JsonProperty("osType")
            String osType;
            @JsonProperty("osArch")
            String osArch;
            @JsonProperty("startTs")
            Long startTs;
            @JsonProperty("buildMultiplier")
            Double buildMultiplier;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ExecutionInfoDTO.ExecutionInfoDTOBuilder.class)
    public static class ExecutionInfoDTO {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("event")
        String event;
        @JsonProperty("branch")
        Branch branch;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Branch.BranchBuilder.class)
    public static class Branch {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("commits")
        List<Commit> commits;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = HarnessNGPipeline.Branch.Commit.CommitBuilder.class)
        public static class Commit {
            @JsonProperty("__recast")
            String recast;
            @JsonProperty("id")
            String id;
            @JsonProperty("link")
            String link;
            @JsonProperty("message")
            String message;
            @JsonProperty("ownerName")
            String ownerName;
            @JsonProperty("ownerId")
            String ownerId;
            @JsonProperty("ownerEmail")
            String ownerEmail;
            @JsonProperty("timeStamp")
            Long timeStamp;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ImageDetails.ImageDetailsBuilder.class)
    public static class ImageDetails {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("imageName")
        String imageName;
        @JsonProperty("imageTag")
        String imageTag;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ScmDetails.ScmDetailsBuilder.class)
    public static class ScmDetails {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("scmUrl")
        String scmUrl;
        @JsonProperty("scmProvider")
        String scmProvider;
        @JsonProperty("scmAuthType")
        String scmAuthType;
        @JsonProperty("scmHostType")
        String scmHostType;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = InfraDetails.InfraDetailsBuilder.class)
    public static class InfraDetails {
        @JsonProperty("__recast")
        String recast;
        @JsonProperty("infraType")
        String infraType;
        @JsonProperty("infraOSType")
        String infraOSType;
        @JsonProperty("infraHostType")
        String infraHostType;
        @JsonProperty("infraArchType")
        String infraArchType;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitDetails.GitDetailsBuilder.class)
    public static class GitDetails {

        @JsonProperty("objectId")
        String objectId;

        @JsonProperty("branch")
        String branch;

        @JsonProperty("repoIdentifier")
        String repoIdentifier;

        @JsonProperty("rootFolder")
        String rootFolder;

        @JsonProperty("filePath")
        String filePath;

        @JsonProperty("repoName")
        String repoName;

        @JsonProperty("commitId")
        String commitId;

        @JsonProperty("fileUrl")
        String fileUrl;

        @JsonProperty("repoUrl")
        String repoUrl;

    }
}