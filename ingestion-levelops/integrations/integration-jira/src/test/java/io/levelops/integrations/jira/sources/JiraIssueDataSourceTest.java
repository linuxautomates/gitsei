package io.levelops.integrations.jira.sources;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraIssue;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JiraIssueDataSourceTest  {
    private Stream<Data<JiraIssue>> getData() throws JsonProcessingException {
        String serialized = "{\"startAt\":0,\"maxResults\":50,\"total\":2,\"issues\":[]}";
        JiraApiSearchResult searchResult = DefaultObjectMapper.get().readValue(serialized, JiraApiSearchResult.class);
        Stream<Data<JiraIssue>> dataStream = searchResult.getIssues().stream()
                .map(BasicData.mapper(JiraIssue.class));
        return dataStream;
    }

    @Test
    public void test() {
        Stream<ImmutablePair<List<JiraIssue>, List<IngestionFailure>>> stream = IntStream.iterate(0, i -> i + 1)
                .mapToObj(pageNumber -> {
                    try {
                        Stream<Data<JiraIssue>> dataStream = getData();
                        return Data.collectData(getData());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .takeWhile(d -> CollectionUtils.isNotEmpty(d.getLeft()));
        List<ImmutablePair<List<JiraIssue>, List<IngestionFailure>>> results = stream.collect(Collectors.toList());
        System.out.println(results.size());
    }
}