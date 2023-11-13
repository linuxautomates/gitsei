package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectRequest;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectResponse;
import io.levelops.api.services.GithubSpotCheckService;
import io.levelops.api.services.GithubSpotCheckService.GithubSpotCheckUserData;
import io.levelops.api.services.GitlabSpotCheckService;
import io.levelops.api.utils.SelfServeEndpointUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.ListUtils;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.util.SpringUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/spotcheck/")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class DataSpotCheckController {

    private final GithubSpotCheckService githubSpotCheckService;
    private final GitlabSpotCheckService gitlabSpotCheckService;

    @Autowired
    public DataSpotCheckController(GithubSpotCheckService githubSpotCheckService, GitlabSpotCheckService gitlabSpotCheckService) {
        this.githubSpotCheckService = githubSpotCheckService;
        this.gitlabSpotCheckService = gitlabSpotCheckService;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubSpotCheckRequest.GithubSpotCheckRequestBuilder.class)
    public static class GithubSpotCheckRequest {
        @JsonProperty("integration_id")
        String integrationId;
        @JsonProperty("from")
        String from; // YYYY-MM-dd (UTC)
        @JsonProperty("to")
        String to; // YYYY-MM-dd (UTC)
        @JsonProperty("limit")
        Integer limit; // optional
        @JsonProperty("users")
        List<String> users; // github logins
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubSpotCheckResponse.GithubSpotCheckResponseBuilder.class)
    public static class GithubSpotCheckResponse {
        @JsonProperty("users")
        List<GithubSpotCheckUserData> users;
    }

    @PostMapping("/github/users")
    public DeferredResult<ResponseEntity<GithubSpotCheckResponse>> doGithubSpotCheck(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @RequestBody GithubSpotCheckRequest request) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            Validate.notBlank(request.getIntegrationId(), "in cannot be null or empty.");
            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(company)
                    .integrationId(request.integrationId)
                    .build();
            return ResponseEntity.ok(GithubSpotCheckResponse.builder()
                    .users(ListUtils.emptyIfNull(request.getUsers()).stream()
                            .map(user -> githubSpotCheckService.fetchUserData(integrationKey, user, request.getFrom(), request.getTo(), request.getLimit()))
                            .collect(Collectors.toList()))
                    .build());
        });
    }

    @PostMapping("/gitlab/projects")
    public DeferredResult<ResponseEntity<GitlabSpotCheckProjectResponse>> doGitlabProjectSpotCheck(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @RequestBody GitlabSpotCheckProjectRequest request) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(gitlabSpotCheckService.fetchProjectData(company, request));
        });
    }
}
