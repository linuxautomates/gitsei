package io.levelops.integrations.checkmarx.services.cxsca;

import io.levelops.integrations.checkmarx.client.cxsca.CxScaClient;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClientException;
import io.levelops.integrations.checkmarx.models.CxScaRiskReportLicense;
import io.levelops.integrations.checkmarx.models.CxScaRiskReportPackage;
import io.levelops.integrations.checkmarx.models.CxScaRiskReportVulnerability;
import io.levelops.integrations.checkmarx.models.CxScaScan;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@Log4j2
public class CxScaScanEnrichmentService {
    private final int forkThreshold;
    private final ForkJoinPool pool;

    public CxScaScanEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    public List<CxScaScan> enrichScans(CxScaClient client, List<CxScaScan> projects) {
        EnrichScanTask enrichProjectTask = new EnrichScanTask(client, projects, forkThreshold);
        return pool.invoke(enrichProjectTask);
    }

    public static class EnrichScanTask extends RecursiveTask<List<CxScaScan>> {

        List<CxScaScan> scans;
        Integer forkThreshold;
        CxScaClient client;

        public EnrichScanTask(CxScaClient client, List<CxScaScan> projects, Integer forkThresold) {
            this.scans = projects;
            this.forkThreshold = forkThreshold;
            this.client = client;
        }

        @Override
        protected List<CxScaScan> compute() {
            if (scans.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichScan();
            }
        }

        public List<CxScaScan> computeInSubTask() {
            int size = scans.size();
            EnrichScanTask enrichProjectTask = new EnrichScanTask(client, scans.subList(0, size / 2), forkThreshold);
            EnrichScanTask enrichProjectTask1 = new EnrichScanTask(client, scans.subList(size / 2, size), forkThreshold);
            enrichProjectTask.fork();
            enrichProjectTask1.fork();
            List<CxScaScan> enrichedProjects = new ArrayList<CxScaScan>(enrichProjectTask.join());
            enrichedProjects.addAll(enrichProjectTask1.join());
            return enrichedProjects;
        }

        public List<CxScaScan> enrichScan() {
            List<CxScaScan> enrichedScans = scans.stream()
                    .map(scan -> {
                        try {
                            List<CxScaRiskReportLicense> licenses = client.getLicenseForRiskReport(scan.getScanId());
                            List<CxScaRiskReportPackage> packages = client.getPackagesForRiskReport(scan.getScanId());
                            List<CxScaRiskReportVulnerability> vulnerabilities = client.getVulnerabilitiesForRiskReport(scan.getScanId());
                            return scan.toBuilder().licenses(licenses)
                                    .vulnerabilities(vulnerabilities)
                                    .packages(packages)
                                    .build();
                        } catch (CxScaClientException e) {
                            log.error("enrichScan: failed to fetch licenses, packages or vulnerabilities for scan: "
                                    + scan.getScanId(), e);
                            return scan;
                        }
                    }).collect(Collectors.toList());
            return enrichedScans;
        }

    }
}
