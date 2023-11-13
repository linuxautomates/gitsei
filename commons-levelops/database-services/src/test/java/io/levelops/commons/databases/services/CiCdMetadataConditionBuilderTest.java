package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class CiCdMetadataConditionBuilderTest {

    private CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;

    @Before
    public void setup() throws SQLException {
        ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
    }

    @Test
    public void testCreateMetadataConditions() {

        Map<String, Object> params = new HashMap<>();
        List<String> criteriaConditions = new ArrayList<>();
        CiCdJobRunsFilter filter = CiCdJobRunsFilter.builder()
                .tags(List.of("delegate"))
                .services(List.of("Deploy_to_GCP"))
                .environments(List.of("CD_only_Docker_Registry_Env"))
                .repositories(List.of("https:///github.com/username/repo"))
                .infrastructures(List.of("CD_only_Docker_Registry_Infrastructure"))
                .deploymentTypes(List.of("Kubernetes"))
                .branches(List.of("branch"))
                .rollback(true)
                .build();
        ciCdMetadataConditionBuilder.prepareMetadataConditions(filter, params, null, criteriaConditions);
        Assert.assertTrue(criteriaConditions.size() == 8);
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("tags")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("service_ids")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("env_ids")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("infra_ids")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("service_types")));
        Assert.assertTrue(criteriaConditions.stream().anyMatch(cond -> cond.contains("boolean")));
    }

    @Test
    public void testCreateMetadataExcludeConditions() {

        Map<String, Object> params = new HashMap<>();
        List<String> criteriaExcludeConditions = new ArrayList<>();
        CiCdJobRunsFilter filter = CiCdJobRunsFilter.builder()
                .excludeTags(List.of("delegate"))
                .excludeServices(List.of("Deploy_to_GCP"))
                .excludeEnvironments(List.of("CD_only_Docker_Registry_Env"))
                .excludeRepositories(List.of("https:///github.com/username/repo"))
                .excludeInfrastructures(List.of("CD_only_Docker_Registry_Infrastructure"))
                .excludeDeploymentTypes(List.of("Kubernetes"))
                .excludeBranches(List.of("branch"))
                .excludeRollback(false)
                .build();
        ciCdMetadataConditionBuilder.prepareMetadataConditions(filter, params, null, criteriaExcludeConditions);
        Assert.assertTrue(criteriaExcludeConditions.size() == 8);
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("tags") && cond.contains("NOT")));
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("service_ids") && cond.contains("NOT")));
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("env_ids") && cond.contains("NOT")));
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("infra_ids") && cond.contains("NOT")));
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("service_types") && cond.contains("NOT")));
        Assert.assertTrue(criteriaExcludeConditions.stream().anyMatch(cond -> cond.contains("rollback") && cond.contains("NOT")));
    }

}