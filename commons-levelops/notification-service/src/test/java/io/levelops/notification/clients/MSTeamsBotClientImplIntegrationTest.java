package io.levelops.notification.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.notification.clients.msteams.MSTeamsBotClientFactory;
import io.levelops.notification.clients.msteams.MSTeamsClientException;
import io.levelops.notification.models.msteams.MSTeamsApiResponse;
import io.levelops.notification.models.msteams.MSTeamsChannel;
import io.levelops.notification.models.msteams.MSTeamsTeam;
import io.levelops.notification.services.MSTeamsService;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MSTeamsBotClientImplIntegrationTest {

    private MSTeamsService msTeamsService;
    private MSTeamsBotClientFactory msTeamsBotClientFactory;

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "msteams1";
    private static final String APPLICATION = "msteams";
    private static final String MSTEAMS_URL = System.getenv("MSTEAMS_URL");
    private static final String MSTEAMS_TOKEN = System.getenv("MSTEAMS_TOKEN");

    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    private static final IntegrationKey KEY = IntegrationKey.builder()
            .tenantId(TENANT_ID)
            .integrationId(INTEGRATION_ID)
            .build();

    @Before
    public void setUp() {
        OkHttpClient okHttpClient = new OkHttpClient();

        msTeamsBotClientFactory = MSTeamsBotClientFactory.builder()
                .inventoryService(new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                        .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, MSTEAMS_URL, Collections.emptyMap(),
                                MoreObjects.firstNonNull(MSTEAMS_TOKEN, "token"), null, null)
                        .build()))
                .okHttpClient(okHttpClient)
                .objectMapper(DefaultObjectMapper.get())
                .build();
        msTeamsService = new MSTeamsService(msTeamsBotClientFactory);
    }

    @Test
    public void getAllTeamsTest() throws MSTeamsClientException, IOException {
        String allTeams = ResourceUtils.getResourceAsString("msteams/all_teams.json");
        MSTeamsApiResponse<MSTeamsTeam> teams = MAPPER.readValue(allTeams,MSTeamsTeam.getJavaType(MAPPER));

        List<MSTeamsTeam> response = msTeamsBotClientFactory.get(KEY).getAllTeams();
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), teams.getValues().size());
    }

    @Test
    public void getChannelsTest() throws MSTeamsClientException, IOException {
        String allChannels = ResourceUtils.getResourceAsString("msteams/all_channels.json");
        MSTeamsApiResponse<MSTeamsTeam> channels = MAPPER.readValue(allChannels,MSTeamsTeam.getJavaType(MAPPER));

        List<MSTeamsChannel> response = msTeamsBotClientFactory.get(KEY).getChannels("44b3d58c-7306-4497-a6b5-46abf9503cae");
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), channels.getValues().size());
    }
}