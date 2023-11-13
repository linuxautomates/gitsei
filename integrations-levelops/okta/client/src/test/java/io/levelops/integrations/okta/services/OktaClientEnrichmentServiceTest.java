package io.levelops.integrations.okta.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class OktaClientEnrichmentServiceTest {

    private OktaClient oktaClient;
    private OktaEnrichmentService enrichmentService;

    @Before
    public void setup() throws OktaClientException {
        oktaClient = Mockito.mock(OktaClient.class);
        enrichmentService = new OktaEnrichmentService(2, 10);
        when(oktaClient.getMembersOfGroup(anyString()))
                .thenReturn(List.of(OktaUser.builder().build()));
    }

    @Test
    public void enrich() {
        List<OktaGroup> groups = enrichmentService.enrichGroups(oktaClient,
                IntegrationKey.builder().build(), List.of(OktaGroup.builder().id(UUID.randomUUID().toString()).build()), true);
        assertThat(groups).isNotNull();
        assertThat(groups).hasSize(1);
        OktaGroup group = groups.get(0);
        assertThat(group.getEnrichedUsers()).hasSize(1);
    }
}
