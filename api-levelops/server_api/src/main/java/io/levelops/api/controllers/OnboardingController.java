package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.ScmRepoRequest;
import io.levelops.api.services.OnboardingService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.models.ScmRepository;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/v1/scm_repos")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;

    @Autowired
    public OnboardingController(OnboardingService onboardingService, ObjectMapper objectMapper, AggCacheService cacheService) {
        this.onboardingService = onboardingService;
        this.mapper = objectMapper;
        this.cacheService = cacheService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/repos/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ScmRepository>>> repositoryDetails(@SessionAttribute(name = "company") String company,
                                                                                              @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
                                                                                              @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {

            String integrationId = (String) filter.getFilter().get("integration_id");
            List<String> filterRepos = getList(filter.getFilter(), "repos");
            Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
            int pageNumber = filter.getPage();
            int pageSize = filter.getPageSize();
            log.info("Discovering repos (or loading from cache) for tenant={}, integration_id={} (page={}, pageSize={})", company, integrationId, pageNumber, pageSize);
            return ResponseEntity.ok(
            PaginatedResponse.of(pageNumber, pageSize,
                    AggCacheUtils.cacheOrCall(disableCache, company, "/repo/list_num"+pageNumber+"_sz"+pageSize+"list", ""+filter.hashCode(), List.of(integrationId),mapper, cacheService,
                            () -> onboardingService.getRepositories(company, integrationId, filterRepos, pageNumber, pageSize)))
            );});
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/search/repo", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ScmRepository>>> searchRepository(@SessionAttribute(name = "company") String company,
                                                                                @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String integrationId = (String) filter.getFilter().get("integration_id");
            String repoName = filter.getFilterValue("repo", String.class).orElse(null);
            String projectKey = filter.getFilterValue("project", String.class).orElse(null);
            int pageNumber = filter.getPage();
            int pageSize = filter.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(pageNumber, pageSize, onboardingService.searchRepository(company, integrationId, repoName,projectKey, pageNumber, pageSize)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, value = "/add/repos", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> addRepositories(@SessionAttribute(name = "company") String company,
                                                                   @RequestBody ScmRepoRequest scmRepoRequest) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(onboardingService.updateRepos(company, scmRepoRequest));
        });

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> getList(Map<String, Object> filter, String key) {
        if (!filter.containsKey(key)) {
            return List.of();
        }
        try {
            if (filter.get(key) instanceof String) {
                String list = (String) filter.get(key);
                String[] splitList = list.split(",");
                return Arrays.asList(splitList);
            } else if (filter.get(key) instanceof List) {
                var values = (Collection) filter.get(key);
                if (CollectionUtils.isEmpty(values)) {
                    return List.of();
                }
                return new ArrayList<String>(values);
            } else {
                return List.of();
            }
        } catch (ClassCastException e) {
            log.error("Unable to get List<String> out the key '{}' in the filters: {}", key, filter, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }

}
