package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.model.spotchecks.GitlabSpotCheckPR;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProject;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectRequest;
import io.levelops.api.model.spotchecks.GitlabSpotCheckProjectResponse;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static io.levelops.api.model.spotchecks.DateUtils.requestStrToDate;

@Service
@Log4j2
public class GitlabSpotCheckService {
    private static final boolean ALLOW_UNSAFE_SSL = true;
    private final GitlabClientFactory clientFactory;

    @Autowired
    public GitlabSpotCheckService(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = new GitlabClientFactory(inventoryService, objectMapper, okHttpClient, 0, ALLOW_UNSAFE_SSL);
    }

    public GitlabSpotCheckProjectResponse fetchProjectData(final String company, final GitlabSpotCheckProjectRequest r) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(r, "integrationKey cannot be null.");
        Validate.notBlank(r.getProjectName(), "project name cannot be null or empty.");
        Validate.notNull(r.getIntegrationId(), "integrationId cannot be null.");
        Validate.notBlank(r.getFrom(), "from cannot be null or empty.");
        Validate.notBlank(r.getTo(), "to cannot be null or empty.");
        IntegrationKey ik = IntegrationKey.builder()
                .tenantId(company).integrationId(String.valueOf(r.getIntegrationId()))
                .build();

        try {
            GitlabClient client = clientFactory.get(ik, true);
            List<GitlabProject> lst = client.getProjectByName(r.getProjectName(), 0, 10000).stream()
                    .filter(p -> r.getProjectName().equals(p.getName()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(lst)) {
                return GitlabSpotCheckProjectResponse.builder()
                        .limit(r.getLimit()).projectNotFound(true)
                        .build();
            }
            Date from = requestStrToDate(r.getFrom());
            Date to = requestStrToDate(r.getTo());

            List<GitlabSpotCheckProject> projects = lst.stream()
                    .map(p -> {
                        List<GitlabSpotCheckPR> prs = client.streamMergeRequests(p.getId(), from, to, 100)
                                .map(GitlabSpotCheckPR::fromGitlabPR)
                                .collect(Collectors.toList());

                        return GitlabSpotCheckProject.fromGitlabProject(p, prs);
                    })
                    .collect(Collectors.toList());

            return GitlabSpotCheckProjectResponse.builder()
                    .projects(projects).limit(r.getLimit())
                    .build();

        } catch (GitlabClientException e) {
            return GitlabSpotCheckProjectResponse.builder()
                    .error(ExceptionPrintout.fromThrowable(e))
                    .build();
        }
    }

}
