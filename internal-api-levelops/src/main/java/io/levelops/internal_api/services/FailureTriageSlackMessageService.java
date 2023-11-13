package io.levelops.internal_api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.models.database.cicd.FailureTriageSlackMessage;
import io.levelops.commons.databases.services.WorkItemFailureTriageViewService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log4j2
@Service
public class FailureTriageSlackMessageService {
    public FailureTriageSlackMessage convertViewToFailureTriageSlackMessage(List<WorkItemFailureTriageViewService.WIFailureTriageView> views) {
        Map<UUID, FailureTriageSlackMessage.JobRun> jobRunsMap = new HashMap<>();
        Map<UUID, List<FailureTriageSlackMessage.RuleHit>> jobRunsHits = new HashMap<>();

        Map<UUID, Map<UUID, FailureTriageSlackMessage.Stage>> stagesMap = new HashMap<>();
        Map<UUID, List<FailureTriageSlackMessage.RuleHit>> stagesHits = new HashMap<>();

        for(WorkItemFailureTriageViewService.WIFailureTriageView view : views) {
            UUID jobRunId = view.getJobRunId();
            FailureTriageSlackMessage.JobRun jobRun = FailureTriageSlackMessage.JobRun.builder()
                    .jobRunId(view.getJobRunId())
                    .jobName(view.getJobName())
                    .jobRunNumber(view.getJobRunNumber())
                    .jenkinsInstanceName(view.getInstanceName())
                    .jenkinsUrl(view.getJobUrl())
                    .runStartTime(view.getRunStartTime())
                    .build();
            //Save Job
            jobRunsMap.put(jobRunId, jobRun);

            FailureTriageSlackMessage.RuleHit ruleHit = FailureTriageSlackMessage.RuleHit.builder()
                    .ruleId(view.getRuleId())
                    .rule(view.getRuleName())
                    .matchesCount(view.getHitsCount())
                    .snippet(view.getSnippet())
                    .build();

            UUID stageId = view.getStageId();
            if(stageId == null) {
                //Save Job Hit
                if(!jobRunsHits.containsKey(jobRunId)) {
                    jobRunsHits.put(jobRunId, new ArrayList<>());
                }
                jobRunsHits.get(jobRunId).add(ruleHit);
            } else {
                FailureTriageSlackMessage.Stage stage = FailureTriageSlackMessage.Stage.builder()
                        .stageId(stageId)
                        .stageName(view.getStageName())
                        .stageJenkinsUrl(view.getStageUrl())
                        .stageStartTime(view.getStageStartTime())
                        .build();
                //Save Stage
                if(!stagesMap.containsKey(jobRunId)){
                    stagesMap.put(jobRunId, new HashMap<>());
                }
                stagesMap.get(jobRunId).put(stageId, stage);

                //Save Stage Hits
                if(!stagesHits.containsKey(stageId)) {
                    stagesHits.put(stageId, new ArrayList<>());
                }
                stagesHits.get(stageId).add(ruleHit);
            }
        }

        List<FailureTriageSlackMessage.JobRun> jobRuns = new ArrayList<>();
        for(UUID jobRunId : jobRunsMap.keySet()) {
            FailureTriageSlackMessage.JobRun currentJobRun = jobRunsMap.get(jobRunId);
            List<FailureTriageSlackMessage.RuleHit> jobRunHits = jobRunsHits.getOrDefault(jobRunId, Collections.emptyList());

            List<FailureTriageSlackMessage.Stage> jobRunStages = new ArrayList<>();
            Map<UUID, FailureTriageSlackMessage.Stage> jobRunStagesMap = stagesMap.getOrDefault(jobRunId, Collections.emptyMap());
            if(jobRunStagesMap != null) {
                for(UUID stageId : jobRunStagesMap.keySet()) {
                    FailureTriageSlackMessage.Stage currentStage = jobRunStagesMap.get(stageId);
                    List<FailureTriageSlackMessage.RuleHit> currentStageHits = stagesHits.getOrDefault(stageId, Collections.emptyList());
                    jobRunStages.add(currentStage.toBuilder().ruleHits(currentStageHits).build());
                }
            }
            Collections.sort(jobRunStages, new Comparator<FailureTriageSlackMessage.Stage>() {
                @Override
                public int compare(FailureTriageSlackMessage.Stage o1, FailureTriageSlackMessage.Stage o2) {
                    return o1.getStageStartTime().compareTo(o2.getStageStartTime());
                }
            });

            jobRuns.add(currentJobRun.toBuilder().ruleHits(jobRunHits).stages(jobRunStages).build());
        }

        Collections.sort(jobRuns, new Comparator<FailureTriageSlackMessage.JobRun>() {
            @Override
            public int compare(FailureTriageSlackMessage.JobRun o1, FailureTriageSlackMessage.JobRun o2) {
                return o1.getRunStartTime().compareTo(o2.getRunStartTime());
            }
        });
        FailureTriageSlackMessage failureTriageSlackMessage = FailureTriageSlackMessage.builder()
                .jobRuns(jobRuns)
                .build();
        try{
            String failureTriageSlackMessageJson = DefaultObjectMapper.get().writeValueAsString(failureTriageSlackMessage);
            log.debug("failureTriageSlackMessage = {}", failureTriageSlackMessageJson);
        } catch (JsonProcessingException e) {
            log.warn("Error serializing failureTriageSlackMessage", e);
        }
        return failureTriageSlackMessage;
    }
}
