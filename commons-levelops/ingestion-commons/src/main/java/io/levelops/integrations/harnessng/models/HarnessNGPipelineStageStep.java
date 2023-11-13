package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGPipelineStageStep.HarnessNGPipelineStageStepBuilder.class)
public class HarnessNGPipelineStageStep {

    @JsonProperty("uuid")
    String uuid;

    @JsonProperty("name")
    String name;

    @JsonProperty("identifier")
    String identifier;

    @JsonProperty("baseFqn")
    String baseFqn;

    @JsonProperty("stepType")
    String stageType;

    @JsonProperty("status")
    String status;

    @JsonProperty("startTs")
    Long startTs;

    @JsonProperty("endTs")
    Long endTs;

    @JsonProperty("outcomes")
    Outcomes outcomes;

    @Value
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Outcomes {
        @JsonProperty("artifact_Docker_Push")
        ArtifactDockerPush artifactDockerPush;

        @JsonProperty("deploymentInfoOutcome")
        DeploymentInfoOutcome deploymentInfoOutcome;

        Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

        /**
         * Required for JsonAnySetter.
         */
        public Outcomes() {
            this(null, null);
        }

        @JsonAnyGetter
        public Map<String, Object> getDynamicFields() {
            return dynamicFields;
        }

        @JsonAnySetter
        public void addDynamicField(String key, Object value) {
            if (key == null || value == null) {
                return;
            }
            dynamicFields.put(key, value);
        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = ArtifactDockerPush.ArtifactDockerPushBuilder.class)
        public static class ArtifactDockerPush {
            @JsonProperty("stepArtifacts")
            StepArtifacts stepArtifacts;

            @Value
            @AllArgsConstructor
            @Builder(toBuilder = true)
            public static class StepArtifacts {

                @JsonProperty("publishedImageArtifacts")
                List<PublishedImageArtifact> publishedImageArtifacts;

                Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

                /**
                 * required for JsonAnySetter
                 */
                public StepArtifacts() {
                    this(null);
                }

                @JsonAnyGetter
                public Map<String, Object> getDynamicFields() {
                    return dynamicFields;
                }

                @JsonAnySetter
                public void addDynamicField(String key, Object value) {
                    if (key == null || value == null) {
                        return;
                    }
                    dynamicFields.put(key, value);
                }

                @Value
                @Builder(toBuilder = true)
                @AllArgsConstructor
                public static class PublishedImageArtifact {
                    @JsonProperty("digest")
                    String digest;
                    @JsonProperty("imageName")
                    String imageName;
                    @JsonProperty("tag")
                    String tag;
                    @JsonProperty("url")
                    String url;

                    Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

                    /**
                     * required for JsonAnySetter
                     */
                    public PublishedImageArtifact() {
                        this(null, null, null, null);
                    }

                    @JsonAnyGetter
                    public Map<String, Object> getDynamicFields() {
                        return dynamicFields;
                    }

                    @JsonAnySetter
                    public void addDynamicField(String key, Object value) {
                        if (key == null || value == null) {
                            return;
                        }
                        dynamicFields.put(key, value);
                    }
                }
            }
        }

        @Value
        @AllArgsConstructor
        @Builder(toBuilder = true)
        public static class DeploymentInfoOutcome {

            @JsonProperty("serverInstanceInfoList")
            List<ServerInstanceInfo> serverInstanceInfoList;

            Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

            /**
             * Required for JsonAnySetter.
             */
            public DeploymentInfoOutcome() {
                this(null);
            }

            @JsonAnyGetter
            public Map<String, Object> getDynamicFields() {
                return dynamicFields;
            }

            @JsonAnySetter
            public void addDynamicField(String key, Object value) {
                if (key == null || value == null) {
                    return;
                }
                dynamicFields.put(key, value);
            }

            @Value
            @AllArgsConstructor
            @Builder(toBuilder = true)
            public static class ServerInstanceInfo {

                @JsonProperty("name")
                String name;
                @JsonProperty("namespace")
                String namespace;
                @JsonProperty("releaseName")
                String releaseName;
                @JsonProperty("podIP")
                String podIP;
                @JsonProperty("containerList")
                List<Container> containerList;

                Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

                /**
                 * Required for JsonAnySetter.
                 */
                public ServerInstanceInfo() {
                    this(null, null, null, null, null);
                }

                @JsonAnyGetter
                public Map<String, Object> getDynamicFields() {
                    return dynamicFields;
                }

                @JsonAnySetter
                public void addDynamicField(String key, Object value) {
                    if (key == null || value == null) {
                        return;
                    }
                    dynamicFields.put(key, value);
                }

                @Value
                @AllArgsConstructor
                @Builder(toBuilder = true)
                public static class Container {
                    @JsonProperty("containerId")
                    String containerId;
                    @JsonProperty("name")
                    String name;
                    @JsonProperty("image")
                    String image;

                    Map<String, Object> dynamicFields = new HashMap<>(); // for future proofing

                    /**
                     * Required for JsonAnySetter.
                     */
                    public Container() {
                        this(null, null, null);
                    }

                    @JsonAnyGetter
                    public Map<String, Object> getDynamicFields() {
                        return dynamicFields;
                    }

                    @JsonAnySetter
                    public void addDynamicField(String key, Object value) {
                        if (key == null || value == null) {
                            return;
                        }
                        dynamicFields.put(key, value);
                    }
                }
            }
        }
    }
}
