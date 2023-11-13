package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.temporary.TempTenableVulnObject;
import io.levelops.commons.databases.services.temporary.TenableVulnsQueryTable;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.tenable.models.NetworkVulnerability;
import io.levelops.integrations.tenable.models.WasVulnerability;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class TenableAggHelper {
    private static final String TENABLE_TRIGGER_VULN_DATATYPE = "vulnerability";
    private static final String TENABLE_TRIGGER_WAS_DATATYPE = "was";

    private final JobDtoParser jobDtoParser;

    @Autowired
    public TenableAggHelper(JobDtoParser jobDtoParser) {
        this.jobDtoParser = jobDtoParser;
    }

    public boolean setupTenableVulnerabilities(String customer, TenableVulnsQueryTable queryTable,
                                               MultipleTriggerResults results) {
        List<TempTenableVulnObject> vulnerabilities = new ArrayList<>();
        return jobDtoParser.applyToResults(customer, TENABLE_TRIGGER_VULN_DATATYPE,
                NetworkVulnerability.class, results.getTriggerResults().get(0),
                vuln -> {
                    TempTenableVulnObject tempTenableVulnObject = TempTenableVulnObject.fromNetworkVuln(vuln);
                    if (tempTenableVulnObject == null) {
                        log.warn("Couldnt parse a tenable vuln object.");
                        return;
                    }
                    vulnerabilities.add(tempTenableVulnObject);
                },
                List.of(() -> {
                    if (vulnerabilities.size() > 0) {
                        queryTable.insertRows(vulnerabilities);
                        vulnerabilities.clear();
                    }
                    return true;
                }));
    }

    public boolean setupTenableWasVulnerabilities(String customer, TenableVulnsQueryTable queryTable,
                                               MultipleTriggerResults results) {
        List<TempTenableVulnObject> vulnerabilities = new ArrayList<>();
        return jobDtoParser.applyToResults(customer, TENABLE_TRIGGER_WAS_DATATYPE,
                WasVulnerability.class, results.getTriggerResults().get(0),
                vuln -> {
                    TempTenableVulnObject tempTenableVulnObject = TempTenableVulnObject.fromWasVuln(vuln);
                    if (tempTenableVulnObject == null) {
                        log.warn("Couldnt parse a tenable WebAppScan(WAS) vuln object.");
                        return;
                    }
                    vulnerabilities.add(tempTenableVulnObject);
                },
                List.of(() -> {
                    if (vulnerabilities.size() > 0) {
                        queryTable.insertRows(vulnerabilities);
                        vulnerabilities.clear();
                    }
                    return true;
                }));
    }
}
