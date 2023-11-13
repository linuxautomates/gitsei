package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.IssueResponse;
import io.levelops.integrations.sonarqube.models.Project;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Log4j2
public class SonarQubeIssueDataSource implements DataSource<Issue, SonarQubeIterativeScanQuery> {

    private final SonarQubeClientFactory sonarQubeClientFactory;

    public SonarQubeIssueDataSource(SonarQubeClientFactory sonarQubeClientFactory) {
        this.sonarQubeClientFactory = sonarQubeClientFactory;
    }

    @Override
    public Data<Issue> fetchOne(SonarQubeIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Issue>> fetchMany(SonarQubeIterativeScanQuery query) throws FetchException {
        SonarQubeClient sonarQubeClient = sonarQubeClientFactory.get(query.getIntegrationKey());
        List<String> componentKeys = getAllComponentKeys(query, sonarQubeClient);
        // The issues search endpoint only supports 10k results.
        // We try to minimize the impact of that by requesting data by each componentKeys and each Type.
        // Then combining the results into a single stream.
        Stream<Data<Issue>> codeSmellIssues = componentKeys.stream().flatMap(componentKey -> {
            log.info("Fetching issues for {} components", componentKey);
            return PaginationUtils.stream(1, 1, offset ->
                    getPagedData(sonarQubeClient, query, offset, "CODE_SMELL", componentKey));
        });
        Stream<Data<Issue>> bugIssues = componentKeys.stream().flatMap(componentKey -> {
            log.info("Fetching issues for {} components", componentKey);
            return PaginationUtils.stream(1, 1, offset ->
                    getPagedData(sonarQubeClient, query, offset, "BUG", componentKey));
        });
        Stream<Data<Issue>> vulnerabilityIssues = componentKeys.stream().flatMap(componentKey -> {
            log.info("Fetching issues for {} components", componentKey);
            return PaginationUtils.stream(1, 1, offset ->
                    getPagedData(sonarQubeClient, query, offset, "VULNERABILITY", componentKey));
        });
        return Stream.concat(Stream.concat(codeSmellIssues, bugIssues), vulnerabilityIssues);
    }

    public static <T> List<List<T>> chunkCollection(Collection<T> collection, int chunkSize) {
        AtomicInteger counter = new AtomicInteger();
        return new ArrayList<List<T>>(collection.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                .values());
    }

    List<String> getAllComponentKeys(SonarQubeIterativeScanQuery query, SonarQubeClient sonarQubeClient) throws SonarQubeClientException {
        if (CollectionUtils.isNotEmpty(query.getProjectKeys())) {
            return new ArrayList<>(query.getProjectKeys());
        } else {
            return PaginationUtils.stream(1, 1, RuntimeStreamException.wrap(page -> {
                        try {
                            return sonarQubeClient.getProjects(query.getUsePrivilegedAPIs(), page).getProjects();
                        } catch (SonarQubeClientException e) {
                            throw new RuntimeStreamException("Failed to get pipelines execution after offset" + page, e);
                        }
                    }))
                    .map(Project::getKey)
                    .collect(Collectors.toList());
        }
    }

    private List<Data<Issue>> getPagedData(
            SonarQubeClient sonarQubeClient,
            SonarQubeIterativeScanQuery query,
            int offset,
            String types,
            String componentKeys) {
        if (offset * (sonarQubeClient.getPageSize() + 1) > 10000) // is the current call going to go above 10000 issues?
        {
            return List.of();
        }
        try {
            IssueResponse issueResponse = sonarQubeClient.getIssues(componentKeys, null, query.getFrom(), query.getTo(), null, null, null, types, offset);
            log.info("IssueResponses count: {} ", issueResponse.getIssues().size());
            return issueResponse.getIssues().stream()
                    .map(BasicData.mapper(Issue.class))
                    .collect(Collectors.toList());
        } catch (SonarQubeClientException e) {
            log.warn("Failed to get issues after page {}", offset, e);
            throw new RuntimeStreamException("Failed to get issues after page=" + offset, e);
        }
    }
}