package io.levelops.api.controllers.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.levelops.api.services.dev_productivity.OrgDevProductivityReportService;
import io.levelops.api.services.dev_productivity.UserDevProductivityReportService;
import io.levelops.api.utils.ForceSourceUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.RelativeScore;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/v1/dev_productivity/reports")
@Log4j2
public class DevProductivityReportController {
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final UserDevProductivityReportService userDevProductivityReportService;
    private final OrgDevProductivityReportService orgDevProductivityReportService;

    private Set<String> esAllowedTenants;
    private Set<String> dbAllowedTenants;

    @Value("${ES_DEV_PROD_TENANTS:}")
    List<String> esDevProdCompanies;

    @Value("${DB_DEV_PROD_TENANTS:}")
    List<String> dbDevProdCompanies;

    @Autowired
    public DevProductivityReportController(ObjectMapper mapper, AggCacheService cacheService, UserDevProductivityReportService userDevProductivityReportService,
                                           OrgDevProductivityReportService orgDevProductivityReportService) {
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.userDevProductivityReportService = userDevProductivityReportService;
        this.orgDevProductivityReportService = orgDevProductivityReportService;
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

    @RequestMapping(method = RequestMethod.POST, value = "/users", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityResponse>>> getReportForUsers(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}", company, requestorEmail, scopes);
            DbListResponse<DevProductivityResponse> response = userDevProductivityReportService.generateReportForUsers(disableCache, requestorEmail, scopes, company, request);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orgs", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityResponse>>> getReportForOrgs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForOrgs company = {}, requestorEmail = {}, scopes = {}", company, requestorEmail, scopes);
            DbListResponse<DevProductivityResponse> response = orgDevProductivityReportService.generateReportForOrgs(disableCache, requestorEmail, scopes, company, request);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/orgs/{org_uuid}/users", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityResponse>>> getReportForOrgUsers(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @PathVariable("org_uuid") String orgUUID,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("getReportForOrgUsers company = {}, requestorEmail = {}, scopes = {}", company, requestorEmail, scopes);
            DbListResponse<DevProductivityResponse> response = orgDevProductivityReportService.generateReportForOrgUsers(disableCache, requestorEmail, scopes, company, orgUUID, request);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/feature_details", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<FeatureBreakDown>>> getFeatureDetails(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<Object> data = List.of(company, request);
            String hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
            log.info("hash = {}", hash);
            DbListResponse<FeatureBreakDown> response = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/dev_productivity/feature_details_",
                    hash, List.of(), mapper, cacheService,
                    () -> userDevProductivityReportService.getFeatureDetails(company, request, forceSource));
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/relative_score", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<RelativeScore>>> getRelativeScores(@RequestParam(name = "there_is_no_cache",
            required = false, defaultValue = "false") Boolean disableCache, @SessionAttribute(name = "company") String company,
             @SessionAttribute(name = "session_user") String requestorEmail, @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
             @RequestBody DefaultListRequest request, @RequestParam(name = "force_source", required = false) String forceSource){

        log.info("getReportForUsers company = {}, requestorEmail = {}, scopes = {}, forceSource = {}", company, requestorEmail, scopes, forceSource);
        Boolean useEs = isUseEs(company, forceSource);
        return SpringUtils.deferResponse(() -> {
            List<Object> data = List.of(company, request);
            String hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
            log.info("hash = {}", hash);
            DbListResponse<RelativeScore> response = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/dev_productivity/users/relative_score",
                    hash, List.of(), mapper, cacheService,
                    () -> userDevProductivityReportService.getRelativeScore(company, requestorEmail, scopes, request, useEs));
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/users/scm_activity", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ScmActivities>>> getSCMActivities(
            @RequestParam(name = "there_is_no_cache",required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String,List<String>> scopes,
            @RequestBody DefaultListRequest request){
        return SpringUtils.deferResponse(() -> {
            boolean valueOnly = true;
            List<Object> data = List.of(company, request);
            String hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
            log.info("hash = {}", hash);
            DbListResponse<ScmActivities> response = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/dev_productivity/users/scm_activity",
                    hash, List.of(), mapper, cacheService,
                    () -> userDevProductivityReportService.getSCMActivity(company, requestorEmail, scopes, request, valueOnly));
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
