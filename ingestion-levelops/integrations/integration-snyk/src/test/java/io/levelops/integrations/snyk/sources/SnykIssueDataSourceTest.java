package io.levelops.integrations.snyk.sources;

import org.junit.Assert;
import org.junit.Test;

public class SnykIssueDataSourceTest {
    @Test
    public void test(){
        SnykIssueDataSource snykIssueDataSource = new SnykIssueDataSource(null);
        Assert.assertEquals(null, snykIssueDataSource.parseScmRepoNamePartial(null));
        Assert.assertEquals(null, snykIssueDataSource.parseScmRepoNamePartial(""));
        Assert.assertEquals("testadmin1-levelops/biojava", snykIssueDataSource.parseScmRepoNamePartial("testadmin1-levelops/biojava:biojava3-core/pom.xml"));
        Assert.assertEquals("testadmin1-levelops/spring-boot", snykIssueDataSource.parseScmRepoNamePartial("testadmin1-levelops/spring-boot:webflux-thymeleaf-serversentevent/pom.xml"));
        Assert.assertEquals("testadmin1-levelops/kafka", snykIssueDataSource.parseScmRepoNamePartial("testadmin1-levelops/kafka:build.gradle"));
        Assert.assertEquals(null, snykIssueDataSource.parseScmRepoNamePartial("testadmin1-levelops/kafkabuild.gradle"));
    }
}