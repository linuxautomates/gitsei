package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ZendeskQueryService {

    private final IntegrationService integService;
    private final ZendeskTicketService zendeskTicketService;
    private final ScmJiraZendeskService scmJiraZendeskService;

    public ZendeskQueryService(IntegrationService integService, ZendeskTicketService zendeskTicketService,
                               ScmJiraZendeskService scmJiraZendeskService) {
        this.integService = integService;
        this.zendeskTicketService = zendeskTicketService;
        this.scmJiraZendeskService = scmJiraZendeskService;
    }

    public DbListResponse<DbZendeskTicket> list(String company, ZendeskTicketsFilter zendeskTicketsFilter,
                                                Map<String, SortingOrder> sortBy, int page, int pageSize) throws SQLException {
        final Map<String, String> integrationUrlMap = buildIntegrationUrlMap(company, zendeskTicketsFilter);
        DbListResponse<DbZendeskTicket> dbListResponse = zendeskTicketService.list(company, zendeskTicketsFilter, sortBy, page, pageSize);
        List<DbZendeskTicket> dbZendeskTicketsWithUrls = dbListResponse.getRecords()
                .stream()
                .map(ticket -> ticket.toBuilder()
                        .ticketUrl(Optional.ofNullable(integrationUrlMap.get(ticket.getIntegrationId()))
                                .map(url -> url + (url.endsWith("/") ? "" : "/") + "agent/tickets/" + ticket.getTicketId())
                                .orElse(null))
                        .build()
                ).collect(Collectors.toList());
        return DbListResponse.of(dbZendeskTicketsWithUrls, dbListResponse.getTotalCount());
    }

    public DbListResponse<ZendeskWithJira> listZendeskTickets(String company,
                                                              JiraIssuesFilter jiraFilter,
                                                              ZendeskTicketsFilter zendeskTicketsFilter,
                                                              Integer pageNum,
                                                              Integer pageSize,
                                                              Map<String, SortingOrder> sortBy,
                                                              OUConfiguration ouConfig) {
        final Map<String, String> integrationUrlMap = buildIntegrationUrlMap(company, zendeskTicketsFilter);
        DbListResponse<ZendeskWithJira> dbListResponse = scmJiraZendeskService.listZendeskTickets(company, jiraFilter,
                zendeskTicketsFilter, pageNum, pageSize, sortBy, ouConfig);
        List<ZendeskWithJira> zendeskWithJiraWithUrls = buildListResponses(integrationUrlMap, dbListResponse);
        return DbListResponse.of(zendeskWithJiraWithUrls, dbListResponse.getTotalCount());
    }

    public DbListResponse<ZendeskWithJira> listZendeskTicketsWithEscalationTime(String company,
                                                                                JiraIssuesFilter jiraFilter,
                                                                                ZendeskTicketsFilter zendeskTicketsFilter,
                                                                                Integer pageNum,
                                                                                Integer pageSize,
                                                                                Map<String, SortingOrder> sortBy,
                                                                                OUConfiguration ouConfig) {
        final Map<String, String> integrationUrlMap = buildIntegrationUrlMap(company, zendeskTicketsFilter);
        DbListResponse<ZendeskWithJira> dbListResponse = scmJiraZendeskService.listZendeskTicketsWithEscalationTime
                (company, jiraFilter, zendeskTicketsFilter, pageNum, pageSize, sortBy, ouConfig);
        List<ZendeskWithJira> zendeskWithJiraWithUrls = buildListResponses(integrationUrlMap, dbListResponse);
        return DbListResponse.of(zendeskWithJiraWithUrls, dbListResponse.getTotalCount());
    }

    public DbListResponse<DbScmFile> listEscalatedFiles(String company,
                                                        ScmFilesFilter scmFilter,
                                                        JiraIssuesFilter jiraFilter,
                                                        ZendeskTicketsFilter zdFilter,
                                                        int page,
                                                        int pageSize,
                                                        Map<String, SortingOrder> sortBy,
                                                        OUConfiguration ouConfig) {
        final Map<String, String> integrationUrlMap = buildIntegrationUrlMap(company, zdFilter);
        DbListResponse<DbScmFile> dbListResponse = scmJiraZendeskService.listEscalatedFiles(company, scmFilter,
                jiraFilter, zdFilter, page, pageSize, sortBy, ouConfig);
        List<DbScmFile> dbScmFiles = dbListResponse.getRecords().stream()
                .map(record -> {
                    List<String> ticketUrls = new ArrayList<>();
                    record.getZendeskTicketIds().forEach(ticketId -> ticketUrls.add(
                            Optional.ofNullable(integrationUrlMap.get(record.getZendeskIntegrationId()))
                                    .map(url -> url + (url.endsWith("/") ? "" : "/") + "agent/tickets/" + ticketId)
                                    .orElse(null)));
                    return record.toBuilder().zendeskTicketUrls(ticketUrls).build();
                }).collect(Collectors.toList());
        return DbListResponse.of(dbScmFiles, dbListResponse.getTotalCount());
    }

    private Map<String, String> buildIntegrationUrlMap(String company, ZendeskTicketsFilter zendeskTicketsFilter) {
        return (zendeskTicketsFilter.getIntegrationIds())
                .stream()
                .map(integId -> integService.get(company, integId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Integration::getId, Integration::getUrl, (integ1, integ2) -> integ2));
    }

    @NotNull
    private List<ZendeskWithJira> buildListResponses(Map<String, String> integrationUrlMap, DbListResponse<ZendeskWithJira> dbListResponse) {
        return dbListResponse.getRecords()
                .stream()
                .map(ticket -> ticket.toBuilder()
                        .ticketUrl(Optional.ofNullable(integrationUrlMap.get(ticket.getIntegrationId()))
                                .map(url -> url + (url.endsWith("/") ? "" : "/") + "agent/tickets/" + ticket.getTicketId())
                                .orElse(null))
                        .build()
                ).collect(Collectors.toList());
    }
}
