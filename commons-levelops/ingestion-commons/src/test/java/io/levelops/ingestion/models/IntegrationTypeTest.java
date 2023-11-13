package io.levelops.ingestion.models;


import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IntegrationTypeTest {
    @Test
    public void testGetIssueManagementIntegrationTypes() {
        EnumSet<IntegrationType> issueManagementIntegrationTypes = IntegrationType.getIssueManagementIntegrationTypes();
        Assert.assertEquals(3, issueManagementIntegrationTypes.size());
        Assert.assertTrue(issueManagementIntegrationTypes.contains(IntegrationType.JIRA));
        Assert.assertTrue(issueManagementIntegrationTypes.contains(IntegrationType.AZURE_DEVOPS));
        Assert.assertTrue(issueManagementIntegrationTypes.contains(IntegrationType.SERVICENOW));
        
        var scmTypes = IntegrationType.getSCMIntegrationTypes();
        Assertions.assertThat(scmTypes).isNotNull();
        Assertions.assertThat(scmTypes).containsExactlyInAnyOrderElementsOf(Set.of(
            IntegrationType.GITHUB,
            IntegrationType.BITBUCKET,
            IntegrationType.BITBUCKET_SERVER,
            IntegrationType.HELIX,
            IntegrationType.AZURE_DEVOPS,
            IntegrationType.GERRIT,
            IntegrationType.GITLAB,
            IntegrationType.HELIX_CORE,
            IntegrationType.HELIX_SWARM
        ));
    }

    @Test
    public void testIsSCMFamily() {
        List<IntegrationType> expected = List.of(IntegrationType.AZURE_DEVOPS, IntegrationType.BITBUCKET_SERVER, IntegrationType.BITBUCKET, IntegrationType.GERRIT, IntegrationType.GITHUB, IntegrationType.GITLAB, IntegrationType.HELIX_CORE, IntegrationType.HELIX_SWARM, IntegrationType.HELIX);
        List<IntegrationType> actual = Arrays.stream(IntegrationType.values()).filter(IntegrationType::isScmFamily).collect(Collectors.toList());
        Assert.assertEquals(expected, actual);
    }
}