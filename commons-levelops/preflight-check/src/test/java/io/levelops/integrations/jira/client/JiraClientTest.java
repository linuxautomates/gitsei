package io.levelops.integrations.jira.client;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraClientTest {

    @Test
    public void url() {
        assertThat(JiraClient.sanitizeUrl("https://levelops.atlassian.net")).isEqualTo("https://levelops.atlassian.net");
        assertThat(JiraClient.sanitizeUrl("http://levelops.atlassian.net")).isEqualTo("https://levelops.atlassian.net");
        assertThat(JiraClient.sanitizeUrl("levelops.atlassian.net")).isEqualTo("https://levelops.atlassian.net");
        assertThat(JiraClient.sanitizeUrl("")).isEqualTo(null);
        assertThat(JiraClient.sanitizeUrl(null)).isEqualTo(null);
    }

}