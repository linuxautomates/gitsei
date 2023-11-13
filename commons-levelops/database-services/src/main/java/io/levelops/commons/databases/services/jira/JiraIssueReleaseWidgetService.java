package io.levelops.commons.databases.services.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Log4j2
@Service
public class JiraIssueReleaseWidgetService {

    private final JiraIssueReleasePrecalculatedWidgetService jiraIssueReleasePrecalculatedWidgetService;
    private final JiraFilterParser jiraFilterParser;
    private final OrgUnitHelper orgUnitHelper;
    private final JiraIssueReleaseService jiraIssueReleaseService;

    private final VelocityConfigsDatabaseService velocityConfigsDatabaseService;

    @Autowired
    public JiraIssueReleaseWidgetService(JiraIssueReleasePrecalculatedWidgetService jiraIssueReleasePrecalculatedWidgetService,
                                         JiraFilterParser jiraFilterParser,
                                         VelocityConfigsDatabaseService velocityConfigsDatabaseService,
                                         final OrgUnitHelper orgUnitHelper,
                                         JiraIssueReleaseService jiraIssueReleaseService) {
        this.jiraIssueReleasePrecalculatedWidgetService = jiraIssueReleasePrecalculatedWidgetService;
        this.jiraFilterParser = jiraFilterParser;
        this.velocityConfigsDatabaseService = velocityConfigsDatabaseService;
        this.orgUnitHelper = orgUnitHelper;
        this.jiraIssueReleaseService = jiraIssueReleaseService;
    }

    private VelocityConfigDTO getVelocityConfig(final String company, DefaultListRequest filter) throws SQLException, NotFoundException {
        String velocityConfigId = filter.getFilterValue("velocity_config_id", String.class).orElse(null);

        VelocityConfig velocityConfig = velocityConfigsDatabaseService.get(company, velocityConfigId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Company %s, Velocity Config Id %s not found!", company, velocityConfigId)
                ));
        VelocityConfigDTO velocityConfigDTO = velocityConfig.getConfig();

        log.info("velocityConfigDTO = {}", velocityConfigDTO);
        return velocityConfigDTO;
    }

    public DbListResponse<JiraReleaseResponse> jiraReleaseTableReport(
            final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult
    ) throws JsonProcessingException, SQLException, BadRequestException, NotFoundException {
        var opt = jiraIssueReleasePrecalculatedWidgetService.jiraReleaseTableReport(
                company, originalRequest, disablePrecalculatedResult
        );
        if (opt.isPresent()) {
            return opt.get();
        }

        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();

        VelocityConfigDTO velocityConfigDTO = getVelocityConfig(company, request);

        JiraSprintFilter jiraSprintFilter = JiraSprintFilter.fromDefaultListRequest(request);
        JiraIssuesFilter issueFilter = jiraFilterParser.createFilter(
                company, request, null, null, null, StringUtils.EMPTY, "", false, true
        );

        final var page = request.getPage();
        final var pageSize = request.getPageSize();

        return jiraIssueReleaseService.jiraReleaseTableReport(
                company, issueFilter, jiraSprintFilter, ouConfig, velocityConfigDTO, page, pageSize
        );
    }

    public DbListResponse<DbJiraIssue> drilldownListReport(
            final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult
    ) throws SQLException, NotFoundException, BadRequestException, JsonProcessingException {
        var opt = jiraIssueReleasePrecalculatedWidgetService.jiraReleaseTableReportDrillDown(
                company, originalRequest, disablePrecalculatedResult
        );
        if (opt.isPresent()) {
            return opt.get();
        }
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();

        final var finalOuConfig = ouConfig;
        VelocityConfigDTO velocityConfigDTO = getVelocityConfig(company, request);

        JiraIssuesFilter issueFilter = jiraFilterParser.createFilter(
                company, request, null, null, null, StringUtils.EMPTY, "", false, true
        );

        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        final var sort = request.getSort();

        return jiraIssueReleaseService.drilldownListReport(
                company, issueFilter, finalOuConfig, velocityConfigDTO, SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())), page, pageSize
        );
    }
}
