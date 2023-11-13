package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.cicd.CiCdPushedArtifacts;
import io.levelops.commons.databases.models.database.cicd.CiCdPushedJobRunParams;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedJobRunParam;
import io.levelops.commons.databases.services.CiCdPushedArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedParamsDatabaseService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@Log4j2
@RequestMapping("/v1/cicd")
@PreAuthorize("hasAuthority('INGESTION')")
public class CiCdPushController {
    private final CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService;
    private final CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService;

    @Autowired
    public CiCdPushController(CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService,
                              CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService) {
        this.ciCdPushedArtifactsDatabaseService = ciCdPushedArtifactsDatabaseService;
        this.ciCdPushedParamsDatabaseService = ciCdPushedParamsDatabaseService;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/push_artifacts", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> pushArtifacts(@SessionAttribute("company") String company,
                                                                                    @RequestBody CiCdPushedArtifacts ciCdPushedArtifacts) {
        return SpringUtils.deferResponse(() -> {
            DbCiCdPushedArtifact dbCiCdPushedArtifact = DbCiCdPushedArtifact.fromCiCdPushedArtifacts(ciCdPushedArtifacts);
            String id = ciCdPushedArtifactsDatabaseService.insertPushedArtifacts(company, dbCiCdPushedArtifact);
            return ResponseEntity.ok(id);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/push_job_run_params", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> pushJobRunParams(@SessionAttribute("company") String company,
                                                                         @RequestBody CiCdPushedJobRunParams ciCdPushedJobRunParams) {
        return SpringUtils.deferResponse(() -> {
            DbCiCdPushedJobRunParam dbCiCdPushedJobRunParam = DbCiCdPushedJobRunParam.fromCiCdPushedJobRunParams(ciCdPushedJobRunParams);
            String id = ciCdPushedParamsDatabaseService.insertPushedJobRunParams(company, dbCiCdPushedJobRunParam);
            return ResponseEntity.ok(id);
        });
    }

}
