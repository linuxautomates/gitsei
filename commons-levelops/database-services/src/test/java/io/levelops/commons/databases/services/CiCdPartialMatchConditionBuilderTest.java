package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.SQLException;

@Log4j2
public class CiCdPartialMatchConditionBuilderTest {

    private CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;

    @Before
    public void setup() throws SQLException {
        ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
    }

    @Test
    public void testAddPartialMatchClause() {

        List<String> criteria = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        String suffix = "application";
        Map<String, Map<String, String>> partialMatchMap = Map.of(
                "tags", Map.of("$begins", "delegate"),
                "service_ids", Map.of("$begins", "Deploy_to_GCP"),
                "env_ids", Map.of("$begins", "CD_only_Docker_Registry_Env"),
                "repo_url", Map.of("$begins", "https:///github.com/username/repo"),
                "infra_ids", Map.of("$be" +
                        "gins", "CD_only_Docker_Registry_Infrastructure"),
                "service_types", Map.of("$ends", "Kubernetes"),
                "branch", Map.of("$ends", "branch"));
        CiCdJobRunsFilter filter = CiCdJobRunsFilter.builder()
                .partialMatch(partialMatchMap)
                .build();
        ciCdPartialMatchConditionBuilder.preparePartialMatchConditions(filter, params, criteria, suffix, null);
        Assert.assertTrue(criteria.size() == 7);
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("tags")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("service_ids")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("env_ids")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("repo_url")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("infra_ids")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("service_types")));
        Assert.assertTrue(criteria.stream().anyMatch(cond -> cond.contains("branch")));
    }
}