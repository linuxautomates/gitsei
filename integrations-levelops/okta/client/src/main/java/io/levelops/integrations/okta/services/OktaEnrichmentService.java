package io.levelops.integrations.okta.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.models.*;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * This class can be used for enriching a {@link List<OktaUser>} and {@link List<OktaGroup>}. A {@link ForkJoinPool} is maintained for performing all
 * the enrichment tasks. The {@link ForkJoinPool} is shared across all the jobs.
 */
@Log4j2
public class OktaEnrichmentService {

    private static final String ID = "id";
    private final int forkThreshold;
    private final ForkJoinPool pool;

    /**
     * all arg constructor
     *
     * @param threadCount   the number of threads for the {@link ForkJoinPool}
     * @param forkThreshold the max number of groups/users to be enriched by each {@link EnrichGroupTask} / {@link EnrichUserTask}
     */
    public OktaEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    /**
     * call this method to enrich {@link OktaGroup}
     *
     * @param oktaClient        {@link OktaClient} used to make calls to Okta
     * @param integrationKey    {@link IntegrationKey} for the job, used for logging purposes
     * @param groups            {@link OktaGroup} to be enriched
     * @param enrichmentEnabled true if group needs to be enriched with other entities, otherwise false
     * @return {@link OktaGroup} enriched groups
     */
    public List<OktaGroup> enrichGroups(OktaClient oktaClient, IntegrationKey integrationKey, List<OktaGroup> groups,
                                        boolean enrichmentEnabled) {
        EnrichGroupTask enrichGroupTask = new EnrichGroupTask(oktaClient, groups, forkThreshold, enrichmentEnabled);
        log.info("enrichGroups: started enriching {} groups for {}", groups.size(), integrationKey);
        return pool.invoke(enrichGroupTask);
    }

    /**
     * call this method to enrich {@link OktaUser}
     *
     * @param oktaClient        {@link OktaClient} used to make calls to Okta
     * @param integrationKey    {@link IntegrationKey} for the job, used for logging purposes
     * @param users             {@link OktaUser} to be enriched
     * @param enrichmentEnabled true if group needs to be enriched with other entities, otherwise false
     * @return {@link OktaUser} enriched users
     */
    public List<OktaUser> enrichUsers(OktaClient oktaClient, IntegrationKey integrationKey, List<OktaUser> users,
                                      List<OktaLinkedObject> linkedObjectDefinitions, List<OktaUserType> userTypes,
                                      boolean enrichmentEnabled) {
        EnrichUserTask enrichUserTask = new EnrichUserTask(oktaClient, users, forkThreshold, enrichmentEnabled,
                linkedObjectDefinitions, userTypes);
        log.info("enrichUsers: started enriching {} users for {}", users.size(), integrationKey);
        return pool.invoke(enrichUserTask);
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<OktaUser>}
     */
    @Log4j2
    static class EnrichUserTask extends RecursiveTask<List<OktaUser>> {

        private final OktaClient oktaClient;
        private final List<OktaUser> users;
        private final int forkThreshold;
        private final boolean enrichmentEnabled;
        private final List<OktaLinkedObject> linkedObjectDefinitions;
        private final List<OktaUserType> userTypes;

        /**
         * all arg constructor
         *
         * @param oktaClient        {@link OktaClient} used to make calls to Okta
         * @param users             {@link OktaUser} to be enriched
         * @param forkThreshold     {@link IntegrationKey} for the job, used for logging purposes
         * @param enrichmentEnabled true if user needs to be enriched with other entities, otherwise false
         */
        public EnrichUserTask(OktaClient oktaClient, List<OktaUser> users, int forkThreshold, boolean enrichmentEnabled,
                              List<OktaLinkedObject> linkedObjectDefinitions, List<OktaUserType> userTypes) {
            this.oktaClient = oktaClient;
            this.users = users;
            this.forkThreshold = forkThreshold;
            this.enrichmentEnabled = enrichmentEnabled;
            this.linkedObjectDefinitions = linkedObjectDefinitions;
            this.userTypes = userTypes;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichUserTask#users} has more than
         * {@link EnrichUserTask#forkThreshold} users
         *
         * @return {@link List<OktaUser>} enriched users
         */
        @Override
        protected List<OktaUser> compute() {
            if (users.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                if (enrichmentEnabled)
                    return enrichUsers();
                return users;
            }
        }

        /**
         * Creates and executes {@link EnrichUserTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<OktaUser>} enriched users
         */
        private List<OktaUser> computeInSubTask() {
            int size = users.size();
            EnrichUserTask enrichUserTask1 = new EnrichUserTask(oktaClient, users.subList(0, size / 2),
                    forkThreshold, enrichmentEnabled, linkedObjectDefinitions, userTypes);
            EnrichUserTask enrichUserTask2 = new EnrichUserTask(oktaClient, users.subList(size / 2, size),
                    forkThreshold, enrichmentEnabled, linkedObjectDefinitions, userTypes);
            enrichUserTask1.fork();
            enrichUserTask2.fork();
            List<OktaUser> enrichedUsers = new ArrayList<>(enrichUserTask1.join());
            enrichedUsers.addAll(enrichUserTask2.join());
            return enrichedUsers;
        }

        /**
         * Enriches each {@link OktaUser}
         *
         * @return {@link List<OktaUser>} enriched Users
         */
        private List<OktaUser> enrichUsers() {
            List<OktaUser> enrichedUsers = this.users.stream()
                    .map(e -> enrichUser(e, linkedObjectDefinitions, userTypes))
                    .collect(Collectors.toList());
            log.debug("enrichUsers: enriched {} users", enrichedUsers.size());
            return enrichedUsers;
        }

        /**
         * Enrich user with linked objects
         *
         * @param user                    user {@link OktaUser} need to be enriched
         * @param linkedObjectDefinitions these are fetched from OktaClient
         * @return {@link OktaUser} enriched
         */
        private OktaUser enrichUser(OktaUser user, List<OktaLinkedObject> linkedObjectDefinitions, List<OktaUserType> userTypes) {
            try {
                user = enrichWithUserType(user, userTypes);
                user = enrichWithAssociations(user, linkedObjectDefinitions);
                List<OktaGroup> groups = oktaClient.getGroupsOfUser(user.getId());
                if (groups.size() > 0) {
                    user = user.toBuilder()
                            .groups(groups.stream()
                                    .map(OktaGroup::getId)
                                    .collect(Collectors.toList()))
                            .build();
                }
                return user;
            } catch (OktaClientException e) {
                log.error("process: encountered client exception while enriching okta user "
                        + e.getMessage(), e);
                return user;
            }
        }

        private OktaUser enrichWithAssociations(OktaUser user, List<OktaLinkedObject> linkedObjectDefinitions) throws OktaClientException {
            List<AssociatedMembers> associatedMembers = new ArrayList<>();
            for (OktaLinkedObject definition : linkedObjectDefinitions) {
                String primaryName = definition.getPrimary().getName();
                String associationName = definition.getAssociated().getName();
                List<String> associates = oktaClient.getLinkedObjectUsers(user.getId(), associationName);
                if (associates.size() > 0) {
                    associatedMembers.add(AssociatedMembers.builder()
                            .primaryName(primaryName)
                            .primaryDescription(definition.getPrimary().getDescription())
                            .primaryTitle(definition.getPrimary().getTitle())
                            .associatedName(associationName)
                            .associatedDescription(definition.getAssociated().getDescription())
                            .associatedTitle(definition.getAssociated().getTitle())
                            .associatedMembers(associates)
                            .build());
                }
            }
            if (associatedMembers.size() > 0) {
                user = user.toBuilder().associatedMembers(associatedMembers).build();
            }
            return user;
        }

        private OktaUser enrichWithUserType(OktaUser user, List<OktaUserType> userTypes) {
            String userTypeId = user.getType().get(ID);
            if (userTypeId != null) {
                List<OktaUserType> matches = userTypes.stream()
                        .filter(ut -> ut.getId().equals(userTypeId))
                        .limit(1)
                        .collect(Collectors.toList());
                if (matches.size() == 1) {
                    user = user.toBuilder().enrichedUserType(matches.get(0)).build();
                }
            }
            return user;
        }
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<OktaGroup>}
     */
    @Log4j2
    static class EnrichGroupTask extends RecursiveTask<List<OktaGroup>> {

        private final OktaClient oktaClient;
        private final List<OktaGroup> groups;
        private final int forkThreshold;
        private final boolean enrichmentEnabled;

        /**
         * all arg constructor
         *
         * @param oktaClient        {@link OktaClient} used to make calls to Okta
         * @param groups            {@link OktaGroup} to be enriched
         * @param forkThreshold     {@link IntegrationKey} for the job, used for logging purposes
         * @param enrichmentEnabled true if user needs to be enriched with other entities, otherwise false
         */
        public EnrichGroupTask(OktaClient oktaClient, List<OktaGroup> groups, int forkThreshold, boolean enrichmentEnabled) {
            this.oktaClient = oktaClient;
            this.groups = groups;
            this.forkThreshold = forkThreshold;
            this.enrichmentEnabled = enrichmentEnabled;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichGroupTask#groups} has more than
         * {@link EnrichGroupTask#forkThreshold} groups
         *
         * @return {@link List<OktaGroup>} enriched groups
         */
        @Override
        protected List<OktaGroup> compute() {
            if (groups.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                if (enrichmentEnabled) {
                    return enrichGroups();
                }
                return groups;
            }
        }

        /**
         * Creates and executes {@link EnrichGroupTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<OktaGroup>} enriched groups
         */
        private List<OktaGroup> computeInSubTask() {
            int size = groups.size();
            EnrichGroupTask enrichGroupSubTask1 = new EnrichGroupTask(oktaClient, groups.subList(0, size / 2),
                    forkThreshold, enrichmentEnabled);
            EnrichGroupTask enrichGroupSubTask2 = new EnrichGroupTask(oktaClient, groups.subList(size / 2, size),
                    forkThreshold, enrichmentEnabled);
            enrichGroupSubTask1.fork();
            enrichGroupSubTask2.fork();
            List<OktaGroup> enrichedGroups = new ArrayList<>(enrichGroupSubTask1.join());
            enrichedGroups.addAll(enrichGroupSubTask2.join());
            return enrichedGroups;
        }

        /**
         * Enriches each {@link OktaGroup}
         *
         * @return {@link List<OktaGroup>} enriched groups
         */
        private List<OktaGroup> enrichGroups() {
            List<OktaGroup> enrichedGroups = this.groups.stream()
                    .map(this::enrichGroup)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} groups", enrichedGroups.size());
            return enrichedGroups;
        }

        /**
         * @param group the {@link OktaGroup} to be enriched
         * @return {@link OktaGroup} the enriched group
         */
        private OktaGroup enrichGroup(OktaGroup group) {
            try {
                String groupId = group.getId();
                List<OktaUser> users = oktaClient.getMembersOfGroup(groupId);
                return group.toBuilder()
                        .enrichedUsers(users)
                        .build();
            } catch (OktaClientException e) {
                log.error("process: encountered client exception while enriching okta groups "
                        + e.getMessage(), e);
                return group;
            }
        }
    }

}
