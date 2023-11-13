package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubIntermediateState.GithubIntermediateStateBuilder.class)
public class GithubIntermediateState {

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
        PRS,
        ISSUES,
        PROJECTS,
        TAGS,
        USERS;

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

    public static GithubIntermediateState parseIntermediateState(Map<String, Object> intermediateState) {
        if (MapUtils.isEmpty(intermediateState)) {
            return GithubIntermediateState.builder().build();
        }
        return DefaultObjectMapper.get().convertValue(intermediateState, GithubIntermediateState.class);
    }

    public static GithubIntermediateState markStageAsCompleted(GithubIntermediateState state, Stage stage) {
        return state.toBuilder()
                .completedStages(ListUtils.addIfNotPresent(state.getCompletedStages(), stage))
                .resumeFromRepo(null) // for next stage, we want to start over
                .build();
    }

    public static JobContext updateJobContext(JobContext context, GithubIntermediateState state) {
        return context.toBuilder()
                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), state))
                .build();
    }

    // endregion
}