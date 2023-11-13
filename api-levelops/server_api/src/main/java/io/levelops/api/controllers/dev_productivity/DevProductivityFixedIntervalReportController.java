package io.levelops.api.controllers.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.dev_productivity.OrgDevProductivityRawStatsReport;
import io.levelops.api.model.dev_productivity.OrgUsersDevProductivityRawStatsReport;
import io.levelops.api.services.dev_productivity.OrgDevProductivityFixedIntervalReportService;
import io.levelops.api.services.dev_productivity.UserDevProductivityFixedIntervalReportService;
import io.levelops.api.utils.ForceSourceUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/v1/dev_productivity/reports/fixed_intervals")
@Log4j2
public class DevProductivityFixedIntervalReportController {
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final UserDevProductivityFixedIntervalReportService userDevProductivityFixedIntervalReportService;
    private final OrgDevProductivityFixedIntervalReportService orgDevProductivityFixedIntervalReportService;

    private Set<String> esAllowedTenants;
    private Set<String> dbAllowedTenants;

    @Value("${ES_DEV_PROD_TENANTS:}")
    List<String> esDevProdCompanies;

    @Value("${DB_DEV_PROD_TENANTS:}")
    List<String> dbDevProdCompanies;

    @Autowired
    public DevProductivityFixedIntervalReportController(ObjectMapper mapper, AggCacheService cacheService, UserDevProductivityFixedIntervalReportService userDevProductivityFixedIntervalReportService,
                                                        OrgDevProductivityFixedIntervalReportService orgDevProductivityFixedIntervalReportService) {
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.userDevProductivityFixedIntervalReportService = userDevProductivityFixedIntervalReportService;
        this.orgDevProductivityFixedIntervalReportService = orgDevProductivityFixedIntervalReportService;
    }

    @PostConstruct
    public void esConfig() {
        this.esAllowedTenants = new HashSet<>();
        this.dbAllowedTenants = new HashSet<>();
        if(CollectionUtils.isNotEmpty(esDevProdCompanies)){
            esAllowedTenants.addAll(esDevProdCompanies);
        }
        if(CollectionUtils.isNotEmpty(dbDevProdCompanies)){
            dbAllowedTenants.addAll(dbDevProdCompanies);
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/users/{user_id_type}/{user_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityResponse>> getReportForSingleUser(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @PathVariable("user_id_type") String userIdType,
            @PathVariable("user_id") String userId,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
            Boolean useEs = isUseEs(company, forceSource);
            DevProductivityResponse response = userDevProductivityFixedIntervalReportService.generateReportForSingleUser(disableCache, requestorEmail, scopes, company, userIdType, userId, request, useEs);
            return ResponseEntity.ok().body(response);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/users/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityResponse>>> getReportForUsers(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
            Boolean useEs = isUseEs(company, forceSource);
            DbListResponse<DevProductivityResponse> response = userDevProductivityFixedIntervalReportService.generateReportForMultipleUsers(disableCache, requestorEmail, scopes, company, request, useEs);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orgs/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityResponse>>> getReportForOrgs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
            Boolean useEs = isUseEs(company, forceSource);
            DbListResponse<DevProductivityResponse> response = orgDevProductivityFixedIntervalReportService.generateReportForOrgs(disableCache, requestorEmail, scopes, company, request, useEs);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/raw_stats/org/users", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgUsersDevProductivityRawStatsReport>>> getRawStatsForOrgUsers(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            log.info("getRawStatsForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
            Boolean useEs = isUseEs(company, forceSource);
            List<OrgUsersDevProductivityRawStatsReport> response = userDevProductivityFixedIntervalReportService.generateRawStatsForUsers(disableCache, requestorEmail, scopes, company, request, useEs);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/raw_stats/orgs", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgDevProductivityRawStatsReport>>> getRawStatsForOrgs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
            Boolean useEs = isUseEs(company, forceSource);
            List<OrgDevProductivityRawStatsReport> response = orgDevProductivityFixedIntervalReportService.generateRawStatsForOrgs(disableCache, requestorEmail, scopes, company, request, useEs);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    private Boolean isUseEs(String company, String forceSource) {
        Boolean forceSourceUseES = ForceSourceUtils.useES(forceSource);
        if(forceSourceUseES != null) {
            log.info("isUseEs forceSourceUseES={}", forceSourceUseES);
            return forceSourceUseES;
        }

        Boolean isDbCompany = this.dbAllowedTenants.contains(company) ;
        log.info("isUseEs isDBCompany={}", isDbCompany);

        return !isDbCompany;
    }
}
