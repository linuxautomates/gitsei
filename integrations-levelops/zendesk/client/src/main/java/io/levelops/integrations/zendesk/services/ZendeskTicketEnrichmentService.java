package io.levelops.integrations.zendesk.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.models.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class can be used for enriching a {@link List<Ticket>}. A {@link ForkJoinPool} is maintained for performing all
 * the enrichment tasks. The {@link ForkJoinPool} is shared across all the jobs.
 */
@Log4j2
public class ZendeskTicketEnrichmentService {

    private final int forkThreshold;
    private final ForkJoinPool pool;

    /**
     * all arg constructor
     *
     * @param threadCount   the number of threads for the {@link ForkJoinPool}
     * @param forkThreshold the max number of tickets to be enriched by each {@link EnrichTicketTask}
     */
    public ZendeskTicketEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    /**
     * call this method to enrich {@link Ticket}
     *
     * @param zendeskClient     {@link ZendeskClient} used to make calls to Zendesk
     * @param integrationKey    {@link IntegrationKey} for the job, used for logging purposes
     * @param tickets           {@link List<Ticket>} to be enriched
     * @param users             {@link List<User>} to be used to enrich tickets
     * @param groups            {@link List<Group>} to be used to enrich tickets
     * @param brands            {@link List<Brand>} to be used to enrich tickets
     * @param organizations     {@link List<Organization>} to be used to enrich tickets
     * @param metrics           {@link List<TicketMetric>}to be used to enrich tickets
     * @param enrichmentEnabled true if ticket needs to be enriched with other entities, otherwise false
     * @param jiralinksEnabled  true if jira links are enabled
     * @return {@link List<Ticket>} enriched tickets
     */
    public List<Ticket> enrichTickets(ZendeskClient zendeskClient, IntegrationKey integrationKey, List<Ticket> tickets,
                                      List<User> users, List<Group> groups, List<Brand> brands,
                                      List<Organization> organizations, List<TicketMetric> metrics,
                                      boolean enrichmentEnabled, boolean jiralinksEnabled) {
        Map<Long, User> usersById = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Group> groupsById = groups.stream().collect(Collectors.toMap(Group::getId, Function.identity()));
        Map<Long, Brand> brandsById = brands.stream().collect(Collectors.toMap(Brand::getId, Function.identity()));
        Map<Long, Organization> organizationsById = organizations.stream().collect(Collectors.toMap(Organization::getId,
                Function.identity()));
        Map<Long, TicketMetric> ticketMetrics = metrics.stream().collect(Collectors.toMap(TicketMetric::getTicketId,
                Function.identity()));
        EnrichTicketTask enrichTicketTask = new EnrichTicketTask(zendeskClient, tickets, usersById, groupsById,
                brandsById, organizationsById, ticketMetrics, forkThreshold, enrichmentEnabled, jiralinksEnabled);
        log.info("enrichTickets: started enriching {} tickets for {}", tickets.size(), integrationKey);
        return pool.invoke(enrichTicketTask);
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<Ticket>}
     */
    @Log4j2
    static class EnrichTicketTask extends RecursiveTask<List<Ticket>> {

        /**
         *
         */
        private static final long serialVersionUID = 504134074322393899L;
        private final ZendeskClient zendeskClient;
        private final List<Ticket> tickets;
        private final Map<Long, User> users;
        private final Map<Long, Group> groups;
        private final Map<Long, Brand> brands;
        private final Map<Long, Organization> organizations;
        private final Map<Long, TicketMetric> ticketMetrics;
        private final int forkThreshold;
        private final boolean enrichmentEnabled;
        private final boolean jiralinksEnabled;

        /**
         * all arg constructor
         *
         * @param zendeskClient     {@link ZendeskClient} used to make calls to zendesk
         * @param tickets           {@link List<Ticket>} to be enriched
         * @param users             {@link List<User>} to be used to enrich tickets
         * @param groups            {@link List<Group>} to be used to enrich tickets
         * @param brands            {@link List<Brand>} to be used to enrich tickets
         * @param organizations     {@link List<Organization>} to be used to enrich tickets
         * @param ticketMetrics     {@link List<TicketMetric>}to be used to enrich tickets
         * @param forkThreshold     {@link IntegrationKey} for the job, used for logging purposes
         * @param enrichmentEnabled true if ticket needs to be enriched with other entities, otherwise false
         * @param jiralinksEnabled  true if jira links are enabled
         */
        EnrichTicketTask(ZendeskClient zendeskClient, List<Ticket> tickets, Map<Long, User> users,
                         Map<Long, Group> groups, Map<Long, Brand> brands, Map<Long, Organization> organizations,
                         Map<Long, TicketMetric> ticketMetrics, int forkThreshold, boolean enrichmentEnabled,
                         boolean jiralinksEnabled) {
            this.zendeskClient = zendeskClient;
            this.tickets = tickets;
            this.users = users;
            this.groups = groups;
            this.brands = brands;
            this.organizations = organizations;
            this.ticketMetrics = ticketMetrics;
            this.forkThreshold = forkThreshold;
            this.enrichmentEnabled = enrichmentEnabled;
            this.jiralinksEnabled = jiralinksEnabled;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichTicketTask#tickets} has more than
         * {@link EnrichTicketTask#forkThreshold} tickets
         *
         * @return {@link List<Ticket>} enriched tickets
         */
        @Override
        protected List<Ticket> compute() {
            if (tickets.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichTickets();
            }
        }

        /**
         * Creates and executes {@link EnrichTicketTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<Ticket>} enriched tickets
         */
        private List<Ticket> computeInSubTask() {
            int size = tickets.size();
            EnrichTicketTask enrichTicketSubTask1 = new EnrichTicketTask(zendeskClient, tickets.subList(0, size / 2),
                    users, groups, brands, organizations, ticketMetrics, forkThreshold, enrichmentEnabled,
                    jiralinksEnabled);
            EnrichTicketTask enrichTicketSubTask2 = new EnrichTicketTask(zendeskClient, tickets.subList(size / 2, size),
                    users, groups, brands, organizations, ticketMetrics, forkThreshold, enrichmentEnabled,
                    jiralinksEnabled);
            enrichTicketSubTask1.fork();
            enrichTicketSubTask2.fork();
            List<Ticket> enrichedTickets = new ArrayList<>(enrichTicketSubTask1.join());
            enrichedTickets.addAll(enrichTicketSubTask2.join());
            return enrichedTickets;
        }

        /**
         * Enriches each {@link Ticket}
         *
         * @return {@link List<Ticket>} enriched tickets
         */
        private List<Ticket> enrichTickets() {
            List<Ticket> enrichedTickets = this.tickets.stream()
                    .map(this::enrichTicket)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} tickets", enrichedTickets.size());
            return enrichedTickets;
        }

        /**
         * Enriches {@code ticket} with {@link List<JiraLink>}, {@link Ticket.RequestAttributes} and
         * {@link List<RequestComment>}
         *
         * @param ticket the {@link Ticket} to be enriched
         * @return {@link Ticket} the enriched ticket
         */
        private Ticket enrichTicket(Ticket ticket) {
            long ticketId = ticket.getId();
            List<JiraLink> jiraLinks = null;
            Ticket.RequestAttributes requestAttributes = null;
            List<RequestComment> comments = null;
            try {
                if (enrichmentEnabled) {
                    if (jiralinksEnabled)
                        jiraLinks = zendeskClient.getJiraLinks(ticketId).getLinks();
                    requestAttributes = zendeskClient.getRequestAttributes(ticketId);
                    comments = zendeskClient.getRequestComments(ticketId).getComments();
                }
            } catch (ZendeskClientException e) {
                log.error("process: encountered client exception while enriching jira links "
                        + e.getMessage(), e);
            }
            return ticket.toBuilder()
                    .jiraLinks(jiraLinks)
                    .requestAttributes(requestAttributes)
                    .requestComments(comments)
                    .ticketMetric(ticketMetrics.get(ticketId))
                    .brand(brands.get(ticket.getBrandId()))
                    .organization(organizations.get(ticket.getOrganizationId()))
                    .requester(users.get(ticket.getRequesterId()))
                    .submitter(users.get(ticket.getSubmitterId()))
                    .assignee(users.get(ticket.getAssigneeId()))
                    .group(groups.get(ticket.getGroupId()))
                    .collaborators(mapToUsers(ticket.getCollaboratorIds()))
                    .ticketFollowers(mapToUsers(ticket.getFollowerIds()))
                    .emailCCs(mapToUsers(ticket.getEmailCCIds()))
                    .build();
        }

        /**
         * maps a list of user ids to a list of users
         *
         * @param userIds {@link List<Long>} list of user ids
         * @return {@link List<User>} corresponding to the {@code userIds}
         */
        @NotNull
        private List<User> mapToUsers(List<Long> userIds) {
            return ListUtils.emptyIfNull(userIds).stream()
                    .map(users::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }
}
