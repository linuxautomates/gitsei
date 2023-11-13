package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraApiSearchQuery;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraMyself;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.PreflightCheckResults.PreflightCheckResultsBuilder;
import lombok.Getter;
import okhttp3.OkHttpClient;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JiraPreflightCheck implements PreflightCheck {

    private static final String PERSONAL = "personal";
    private static final boolean ALLOW_UNSAFE_SSL = true;

    @Getter
    private final String integrationType = "jira";
    private final JiraClientFactory clientFactory;

    @Autowired
    public JiraPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = JiraClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .allowUnsafeSSL(ALLOW_UNSAFE_SSL)
                .build();
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .tenantId(tenantId)
                .integration(integration)
                .allChecksMustPass();

        JiraClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException | NullPointerException e) {
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }

        // LEV-4451 disabling /myself check
        // builder.check(checkMyself(client));
        if (BooleanUtils.isNotTrue((Boolean) MapUtils.emptyIfNull(integration.getMetadata()).get(PERSONAL))) {
            builder.check(checkProject(client));
        }
        builder.check(checkSearch(client));

        return builder.build();
    }

    private PreflightCheckResult checkMyself(JiraClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("/myself")
                .success(true);
        try {
            JiraMyself myself = client.getMyself();
            if (myself == null || Strings.isEmpty(myself.getAccountId())) {
                check.success(false)
                        .error("Response from /myself did not contain account_id");
            }
        } catch (JiraClientException e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkProject(JiraClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("/project")
                .success(true);
        try {
            List<JiraProject> output = client.getProjects();
            if (CollectionUtils.isEmpty(output)) {
                check.success(false)
                        .error("Response from /project did not return any project");
            }
        } catch (JiraClientException e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkSearch(JiraClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("/search")
                .success(true);
        try {
            JiraApiSearchResult output = client.search(JiraApiSearchQuery.builder()
                    .maxResults(5)
                    .build());
            if (CollectionUtils.isEmpty(output.getIssues())) {
                check.success(false)
                        .error("Response from /search did not return any issue");
            }
        } catch (JiraClientException e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }
}
