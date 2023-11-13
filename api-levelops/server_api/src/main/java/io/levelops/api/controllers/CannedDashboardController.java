package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.dashboard.CloneDashboardRequest;
import io.levelops.commons.databases.models.database.dashboard.CloneDashboardResponse;
import io.levelops.commons.databases.services.CannedDashboardService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Set;

@RestController
@RequestMapping("/v1/canned_dashboards")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class CannedDashboardController {

    private static final Set<String> PERMITTED_TENANTS = Set.of("foo", "levelops");

    private final CannedDashboardService cannedDashboardService;

    @Autowired
    public CannedDashboardController(CannedDashboardService cannedDashboardService) {
        this.cannedDashboardService = cannedDashboardService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<CloneDashboardResponse> cloneDashboard(@RequestBody CloneDashboardRequest cloneDashboardRequest,
                                                                 @SessionAttribute("company") String company) throws BadRequestException, SQLException {
        if (StringUtils.isEmpty(cloneDashboardRequest.getDashboardName()) && StringUtils.isEmpty(cloneDashboardRequest.getDashboardId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name or id.");
        }
        if (!PERMITTED_TENANTS.contains(company)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }
        return ResponseEntity.ok(cannedDashboardService.cloneDashboard(cloneDashboardRequest));
    }
}

