package io.levelops.integrations.azureDevops.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Streams;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.models.AzureDevopsIntermediateState;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.ProjectProperty;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
public class AzureDevopsProjectProviderService {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    public Stream<Project> streamProjects(IngestionCachingService ingestionCachingService,
                                          AzureDevopsClient azureDevopsClient,
                                          AzureDevopsIterativeScanQuery query,
                                          AzureDevopsIntermediateState intermediateState) throws RuntimeStreamException {
        List<String> qualifiedProjects = azureDevopsClient.getQualifiedProjects();
        log.info("List of qualified projects : {}", qualifiedProjects);

        // remaining projects in current org (might be empty if not resuming)
        String resumeFromOrganization = intermediateState.getResumeFromOrganization();
        String resumeFromProject = intermediateState.getResumeFromProject();
        Stream<Project> remainingProjectsInCurrentOrg = getRemainingProjectsInCurrentOrg(ingestionCachingService,
                azureDevopsClient, query.getIntegrationKey(), resumeFromOrganization, resumeFromProject);

        // all projects in all remaining orgs
        List<String> remainingOrgs = getRemainingOrgs(azureDevopsClient.getOrganizations(), resumeFromOrganization);
        Stream<Project> projectsInRemainingOrgs = remainingOrgs.stream()
                .flatMap(RuntimeStreamException.wrap(currentOrg ->
                        streamAllProjectsInOrg(ingestionCachingService, azureDevopsClient, query.getIntegrationKey(), currentOrg)));

        return Streams.concat(remainingProjectsInCurrentOrg, projectsInRemainingOrgs)
                .filter(Objects::nonNull)
                .filter(project -> {
                    if (ListUtils.isEmpty(qualifiedProjects)) {
                        // if the filter was not specified, we do not want to filter anything
                        return true;
                    }
                    String organization = StringUtils.defaultString(project.getOrganization()).trim().toLowerCase();
                    String projectName = StringUtils.defaultString(project.getName()).trim().toLowerCase();
                    String qualifiedProjectName = organization + "/" + projectName;
                    return qualifiedProjects.contains(qualifiedProjectName);
                });
    }


    @NotNull
    protected Stream<Project> getRemainingProjectsInCurrentOrg(IngestionCachingService ingestionCachingService,
                                                               AzureDevopsClient azureDevopsClient,
                                                               IntegrationKey integrationKey,
                                                               String currentOrg,
                                                               String resumeFromProject) {
        Supplier<Stream<Project>> projectStreamSupplier = () -> {
            try {
                return streamAllProjectsInOrg(ingestionCachingService, azureDevopsClient, integrationKey, currentOrg);
            } catch (IOException e) {
                throw new RuntimeStreamException("Could not stream projects for org=" + currentOrg, e);
            }
        };
        return doGetRemainingProjectsInCurrentOrg(projectStreamSupplier, currentOrg, resumeFromProject);
    }

    /**
     * Get stream of remaining projects in current org.
     * <p>
     * - If current org is missing, returns an empty stream.
     * - If resumeFromProject is missing, returns all projects in current org.
     * - Otherwise, returns all projects in current org that are after resumeFromProject (including resumeFromProject itself)
     */
    @NotNull
    protected static Stream<Project> doGetRemainingProjectsInCurrentOrg(Supplier<Stream<Project>> projectStreamSupplier,
                                                                        String currentOrg,
                                                                        String resumeFromProject) {
        if (StringUtils.isBlank(currentOrg)) {
            return Stream.empty();
        }
        Stream<Project> projectStream = projectStreamSupplier.get();
        if (StringUtils.isBlank(resumeFromProject)) {
            return projectStream;
        }
        Iterator<Project> projectIterator = projectStream.iterator();
        MutableBoolean skipProjects = new MutableBoolean(true);
        return Stream.generate(() -> {
                    while (projectIterator.hasNext()) {
                        Project nextProject = projectIterator.next();
                        if (skipProjects.isFalse()) {
                            return nextProject;
                        }
                        if (resumeFromProject.equalsIgnoreCase(nextProject.getName())) {
                            log.info("Resuming scan from org={}, project={}", currentOrg, resumeFromProject);
                            skipProjects.setFalse();
                            return nextProject;
                        }
                    }
                    return null;
                })
                .takeWhile(Objects::nonNull);
    }

    @NotNull
    protected static List<String> getRemainingOrgs(List<String> organizations, String resumeFromOrganization) {
        if (StringUtils.isBlank(resumeFromOrganization)) {
            return organizations;
        }
        int fromIndex = organizations.indexOf(resumeFromOrganization);
        if (fromIndex == -1) {
            log.info("Could not find an org '{}' to resume from. Will return all the orgs to be safe.", resumeFromOrganization);
            return organizations;
        }
        log.info("Will skip organizations before '{}'", resumeFromOrganization);
        return organizations.subList(fromIndex + 1, organizations.size());
    }

    Stream<Project> streamAllProjectsInOrg(IngestionCachingService ingestionCachingService,
                                           AzureDevopsClient azureDevopsClient,
                                           IntegrationKey integrationKey,
                                           String currentOrg) throws IOException {
        if (ingestionCachingService.isEnabled()) {
            // stream from cache if already available
            Optional<Stream<Project>> projectStreamOpt = getProjectStreamFromCache(ingestionCachingService, integrationKey, currentOrg);
            if (projectStreamOpt.isPresent()) {
                return projectStreamOpt.get();
            }
            // fetch from cloud and write to cache
            cacheAllProjectsInOrg(ingestionCachingService, azureDevopsClient, integrationKey, currentOrg);

            // stream from cache
            projectStreamOpt = getProjectStreamFromCache(ingestionCachingService, integrationKey, currentOrg);
            if (projectStreamOpt.isPresent()) {
                return projectStreamOpt.get();
            }


            log.info("Something went wrong while caching projects for org={}. Will fetch directly from cloud.", currentOrg);
        }

        // if not cached or caching didn't work, stream directly from cloud
        return streamAllProjectsInOrgFromCloud(azureDevopsClient, integrationKey, currentOrg);
    }

    private Optional<Stream<Project>> getProjectStreamFromCache(IngestionCachingService ingestionCachingService,
                                                                IntegrationKey integrationKey,
                                                                String currentOrg) {
        log.info("Attempting to read ADO projects from cache for org={}", currentOrg);
        String countKey = getProjectCountKey(currentOrg);
        Optional<String> countOpt = ingestionCachingService.read(integrationKey.getTenantId(), integrationKey.getIntegrationId(), countKey);
        if (countOpt.isEmpty()) {
            log.info("Could not find any project cached for org={}", currentOrg);
            return Optional.empty();
        }

        int count = NumberUtils.toInteger(countOpt.get(), 0);
        if (count <= 0) {
            log.info("Cache indicated that org={} is empty", currentOrg);
            return Optional.of(Stream.empty());
        }

        log.info("Found {} cached projects for org={}", count, currentOrg);

        return Optional.of(IntStream.range(0, count).<Project>mapToObj(index -> {
            String projectKey = getProjectKey(currentOrg, index);
            Optional<String> projectOpt = ingestionCachingService.read(integrationKey.getTenantId(), integrationKey.getIntegrationId(), projectKey);
            if (projectOpt.isEmpty()) {
                return null;
            }
            try {
                return MAPPER.readValue(projectOpt.get(), Project.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize ADO project from cache for company={}, key={}", integrationKey, projectKey, e);
                return null;
            }
        }).filter(Objects::nonNull));
    }

    private void cacheAllProjectsInOrg(IngestionCachingService ingestionCachingService,
                                       AzureDevopsClient azureDevopsClient,
                                       IntegrationKey integrationKey,
                                       String currentOrg) throws IOException {
        log.info("Fetching and writing projects to cache for org={}", currentOrg);

        Stream<Project> projectStream = streamAllProjectsInOrgFromCloud(azureDevopsClient, integrationKey, currentOrg);
        MutableInt index = new MutableInt(0);
        try {
            projectStream.forEach(project -> {
                try {
                    String key = getProjectKey(currentOrg, index.intValue());
                    String value = MAPPER.writeValueAsString(project);
                    ingestionCachingService.write(integrationKey.getTenantId(), integrationKey.getIntegrationId(), key, value);
                        index.increment();

                } catch (IOException e) {
                    throw new RuntimeStreamException(e);
                }
            });
        } catch (RuntimeStreamException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof IOException) {
                throw (IOException) rootCause;
            }
            throw new IOException("Failed to cache projects for org=" + currentOrg, e.getCause());
        }

        // store count
        String key = getProjectCountKey(currentOrg);
        String value = String.valueOf(index.intValue());
        ingestionCachingService.write(integrationKey.getTenantId(), integrationKey.getIntegrationId(), key, value);

        log.info("Fetched and wrote {} projects to cache for org={}", index.intValue(), currentOrg);
    }

    private static String getProjectKey(String org, int index) {
        return String.format("ado_org_%s_project_nb_%s", org, index);
    }

    private static String getProjectCountKey(String org) {
        return String.format("ado_org_%s_project_count", org);
    }

    private Stream<Project> streamAllProjectsInOrgFromCloud(AzureDevopsClient azureDevopsClient,
                                                            IntegrationKey integrationKey,
                                                            String currentOrg) {
        log.info("Fetching projects from Cloud for org={}", currentOrg);
        return PaginationUtils.stream(STARTING_CURSOR, continuationToken -> {
            if (continuationToken == null) {
                return null;
            }
            return getOnePageOfProjects(azureDevopsClient, integrationKey, currentOrg, continuationToken);
        });
    }

    private PaginationUtils.CursorPageData<Project> getOnePageOfProjects(AzureDevopsClient azureDevopsClient,
                                                                         IntegrationKey integrationKey,
                                                                         String organization,
                                                                         String continuationToken) {
        try {
            ProjectResponse projectResponse;
            Stopwatch sw = Stopwatch.createStarted();
            try {
                projectResponse = azureDevopsClient.getProjects(organization, continuationToken);
                log.info("azure ingestion timing getProjects = {}, org = {}, projectResponse.size() = {}", sw.elapsed(TimeUnit.SECONDS), organization, CollectionUtils.emptyIfNull(projectResponse.getProjects()).size());
            } catch (AzureDevopsClientException e) {
                log.warn("Failed to get projects after page {}", continuationToken, e);
                log.info("azure ingestion timing getProjects = {}, org = {}, failed", sw.elapsed(TimeUnit.SECONDS), organization);
                throw new RuntimeStreamException("Failed to get projects after page=" + continuationToken, e);
            } finally {
                sw.stop();
            }
            String nextContinuationToken = projectResponse.getContinuationToken() == null ? null :
                    projectResponse.getContinuationToken();
            List<Project> projects = CollectionUtils.emptyIfNull(projectResponse.getProjects()).stream()
                    .filter(project -> project.getLastUpdateTime() != null)
                    .map(project -> {
                        List<ProjectProperty> projectProperties = null;
                        Stopwatch swProjectProperties = Stopwatch.createStarted();
                        try {
                            projectProperties = azureDevopsClient.getProjectProperties(organization, project.getId());
                            log.info("azure ingestion timing getProjectProperties = {}, org = {}, project = {}, projectProperties.size() = {}", swProjectProperties.elapsed(TimeUnit.SECONDS), organization, project.getId(), CollectionUtils.emptyIfNull(projectProperties).size());
                            project = getProjectVcs(project, projectProperties);
                        } catch (AzureDevopsClientException e) {
                            log.warn("Failed to fetch project properties for project " + project + " organization " + project.getOrganization(), e);
                            log.info("azure ingestion timing getProjectProperties = {}, org = {}, project = {}, failed", swProjectProperties.elapsed(TimeUnit.SECONDS), organization, project.getId());
                            throw new RuntimeStreamException("Encountered error while enriching projects :" + project + " organization :" + project.getOrganization() +
                                    integrationKey, e);
                        } finally {
                            swProjectProperties.stop();
                        }
                        return project.toBuilder()
                                .projectProperty(projectProperties)
                                .build();
                    })
                    .collect(Collectors.toList());
            return PaginationUtils.CursorPageData.<Project>builder()
                    .data(projects)
                    .cursor(nextContinuationToken)
                    .build();
        } catch (RuntimeStreamException e) {
            log.error("Encountered error while enriching projects for integration key: "
                    + integrationKey + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Encountered error while enriching projects for integration key: " +
                    integrationKey, e);
        }
    }

    private Project getProjectVcs(Project project, List<ProjectProperty> projectProperties) {
        AtomicBoolean isGit = new AtomicBoolean(false);
        AtomicBoolean isTfvc = new AtomicBoolean(false);
        projectProperties.forEach(projectProperty -> {
            if ("System.SourceControlTfvcEnabled".equals(projectProperty.getName())) {
                isTfvc.set(true);
            } else if ("System.SourceControlGitEnabled".equals(projectProperty.getName())) {
                isGit.set(true);
            }
        });
        return project.toBuilder()
                .gitEnabled(isGit.get())
                .tfvcEnabled(isTfvc.get())
                .build();
    }
}
