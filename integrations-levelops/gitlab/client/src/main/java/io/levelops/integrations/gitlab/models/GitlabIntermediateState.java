package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabIntermediateState.GitlabIntermediateStateBuilder.class)
public class GitlabIntermediateState {
    @JsonProperty("completed_stages")
    List<Stage> completedStages;

    @JsonProperty("resume_from_repo")
    String resumeFromRepo;

    public List<Stage> getCompletedStages() {
        return ListUtils.emptyIfNull(completedStages);
    }

    @JsonIgnore
    public boolean isResuming() {
        return CollectionUtils.isNotEmpty(completedStages) || StringUtils.isNotEmpty(resumeFromRepo);
    }

    public enum Stage {
        COMMITS,
        TAGS,
        MRS,
        USERS,
        MILESTONES,
        GROUPS,
        PIPELINES,
        ISSUES,
        PROJECTS;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public static Stage fromString(String value) {
            return EnumUtils.getEnumIgnoreCase(Stage.class, value);
        }
    }

    // region utils

    public static GitlabIntermediateState parseIntermediateState(Map<String, Object> intermediateState) {
        if (MapUtils.isEmpty(intermediateState)) {
            return GitlabIntermediateState.builder().build();
        }
        return DefaultObjectMapper.get().convertValue(intermediateState, GitlabIntermediateState.class);
    }

    public static GitlabIntermediateState markStageAsCompleted(GitlabIntermediateState state, Stage stage) {
        return state.toBuilder()
                .completedStages(ListUtils.addIfNotPresent(state.getCompletedStages(), stage))
                .resumeFromRepo(null) // for next stage, we want to start over
                .build();
    }

    public static JobContext updateJobContext(JobContext context, GitlabIntermediateState state) {
        return context.toBuilder()
                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), state))
                .build();
    }

    public static void updateIntermediateState(String projectName, IntermediateStateUpdater intermediateStateUpdater) {
        try {
            var oldState = intermediateStateUpdater.getIntermediateState();
            var newState = GitlabIntermediateState.parseIntermediateState(oldState).toBuilder()
                    .resumeFromRepo(projectName)
                    .build();
            intermediateStateUpdater.updateIntermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), newState));
        } catch (Exception e) {
            log.error("Failed to update intermediate state", e);
        }
    }
}
