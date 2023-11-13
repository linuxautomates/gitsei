package io.levelops.integrations.harnessng.client;

import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class HarnessNGClientResilienceTest {
    public static final int PAGE = 0;

    public static final String accountIdentifier = "testAccount";
    public static final String projectIdentifier = "testProject";
    public static final String orgIdentifier = "testOrg";
    public static HarnessNGClient client;

    @Before
    public void setup() throws HarnessNGClientException {
        client = Mockito.mock(HarnessNGClient.class);

        when(client.streamProjects(accountIdentifier)).thenCallRealMethod();
        when(client.getProjects(accountIdentifier, PAGE)).thenThrow(new HarnessNGClientException("Not Authorised"));

        when(client.streamExecutions(accountIdentifier, projectIdentifier, orgIdentifier, null, null)).thenCallRealMethod();
        when(client.getExecutions(accountIdentifier, projectIdentifier, orgIdentifier, PAGE, null, null))
                .thenThrow(new HarnessNGClientException("Not Authorised"));
    }

    @Test
    public void testResilience() {
        List<HarnessNGProject> projects = client.streamProjects("").collect(Collectors.toList());
        assertThat(projects).hasSize(0);

        List<HarnessNGPipeline> pipelines = client.streamExecutions(accountIdentifier, projectIdentifier, "", null, null).collect(Collectors.toList());
        assertThat(pipelines).hasSize(0);
    }
}
