package io.levelops.integrations.checkmarx.services.cxsca;

import io.levelops.integrations.checkmarx.client.cxsca.CxScaClient;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClientException;
import io.levelops.integrations.checkmarx.models.CxScaProject;
import io.levelops.integrations.checkmarx.models.CxScaRiskReport;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Log4j2
public class CxScaProjectEnrichmentService {
    private final int forkThreshold;
    private final ForkJoinPool pool;

    public CxScaProjectEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    public List<CxScaProject> enrichProjects(CxScaClient client, List<CxScaProject> projects) {
        EnrichProjectTask enrichProjectTask = new EnrichProjectTask(client, projects, forkThreshold);
        return pool.invoke(enrichProjectTask);
    }

    public static class EnrichProjectTask extends RecursiveTask<List<CxScaProject>> {

        List<CxScaProject> projects;
        Integer forkThreshold;
        CxScaClient client;

        public EnrichProjectTask(CxScaClient client, List<CxScaProject> projects, Integer forkThresold) {
            this.projects = projects;
            this.forkThreshold = forkThreshold;
            this.client = client;
        }

        @Override
        protected List<CxScaProject> compute() {
            if (projects.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichProject();
            }
        }

        public List<CxScaProject> computeInSubTask() {
            int size = projects.size();
            EnrichProjectTask enrichProjectTask = new EnrichProjectTask(client, projects.subList(0, size / 2), forkThreshold);
            EnrichProjectTask enrichProjectTask1 = new EnrichProjectTask(client, projects.subList(size / 2, size), forkThreshold);
            enrichProjectTask.fork();
            enrichProjectTask1.fork();
            List<CxScaProject> enrichedProjects = new ArrayList<CxScaProject>(enrichProjectTask.join());
            enrichedProjects.addAll(enrichProjectTask1.join());
            return enrichedProjects;
        }

        public List<CxScaProject> enrichProject() {
            List<CxScaProject> enrichedProjects = projects.stream()
                    .map(project -> {
                        try {
                            CxScaRiskReport report = client.getRiskReportSummaries(project.getName(), Integer.MAX_VALUE);
                            return project.toBuilder().riskReport(report).build();
                        } catch (CxScaClientException e) {
                            log.error("enrichProject: failed to fetch risk report summaries for project: "
                                    + project.getName(), e);
                            return project;
                        }
                    }).collect(Collectors.toList());
            return enrichedProjects;
        }

    }
}
