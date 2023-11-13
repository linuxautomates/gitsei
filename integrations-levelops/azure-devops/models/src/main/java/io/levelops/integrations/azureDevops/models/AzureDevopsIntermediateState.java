package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.models.JobContext;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsIntermediateState.AzureDevopsIntermediateStateBuilder.class)
public class AzureDevopsIntermediateState {

    @JsonProperty("completed_stages")
    List<Stage> completedStages;

    @JsonProperty("resume_from_org")
    String resumeFromOrganization;

    @JsonProperty("resume_from_project")
    String resumeFromProject;

    public List<Stage> getCompletedStages() {
        return ListUtils.emptyIfNull(completedStages);
    }

    public enum Stage {
        COMMITS,
        PRS,
        TEAMS,
        ITERATIONS,
        PIPELINES,
        BUILDS,
        RELEASES,
        CHANGESETS,
        METADATA,
        LABELS,
        BRANCHES,
        WORKITEMS,
        WORKITEM_HISTORIES,
        WORKITEM_FIELDS,
        TAGS;

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

    public static AzureDevopsIntermediateState parseIntermediateState(Map<String, Object> intermediateState) {
        if (MapUtils.isEmpty(intermediateState)) {
            return AzureDevopsIntermediateState.builder().build();
        }
        return DefaultObjectMapper.get().convertValue(intermediateState, AzureDevopsIntermediateState.class);
    }

    public static AzureDevopsIntermediateState markStageAsCompleted(AzureDevopsIntermediateState state, Stage stage) {
        return state.toBuilder()
                .completedStages(ListUtils.addIfNotPresent(state.getCompletedStages(), stage))
                .resumeFromProject(null) // for next stage, we want to start over
                .resumeFromOrganization(null)
                .build();
    }

    public static JobContext updateJobContext(JobContext context, AzureDevopsIntermediateState state) {
        return context.toBuilder()
                .intermediateState(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), state))
                .build();
    }

    // endregion
}
