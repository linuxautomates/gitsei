package io.levelops.commons.inventory.oauth;

import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class InventoryOauthTokenProviderTest {

    @Mock
    InventoryServiceImpl inventoryService;

    Token token;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        token = Token.builder()
                .tokenData(OauthToken.builder()
                        .token("abc")
                        .build())
                .build();

        when(inventoryService.getToken(eq("tenant"), eq("csUid"), eq("token1")))
                .thenReturn(token);
    }

    @Test
    public void token() {
        InventoryOauthTokenProvider inventoryOauthTokenProvider = new InventoryOauthTokenProvider(
                inventoryService,
                IntegrationKey.builder()
                        .tenantId("tenant")
                        .integrationId("csUid")
                        .build(),
                "token1", token);
        assertThat(inventoryOauthTokenProvider.getToken()).isEqualTo("abc");
    }
}