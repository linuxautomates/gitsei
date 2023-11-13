package io.levelops.integrations.helixcore.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import io.levelops.integrations.helixcore.models.HelixCoreFile;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class HelixCoreClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "helixcore1";
    private static final String APPLICATION = "helixcore";
    private static final String HELIX_CORE_HOST = System.getenv("HELIX_CORE_URL");
    private static final String HELIX_CORE_USERNAME = System.getenv("HELIX_CORE_USERNAME");
    private static final String HELIX_CORE_PASSWORD = System.getenv("HELIX_CORE_PASSWORD");
    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private HelixCoreClientFactory helixcoreClientFactory;

    @Before
    public void setup() {
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, HELIX_CORE_HOST, null, HELIX_CORE_USERNAME, HELIX_CORE_PASSWORD)
                .build());
        helixcoreClientFactory = HelixCoreClientFactory.builder()
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getDepots() throws HelixCoreClientException {
        var depotsResponse = helixcoreClientFactory.get(TEST_INTEGRATION_KEY).getDepots();
        List<HelixCoreDepot> depots = depotsResponse.collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(depots);
        assertThat(depots).isNotNull();
    }

    @Test
    public void getChangeLists() throws HelixCoreClientException {
        ZoneId zoneId = ZoneId.of("UTC");

        Instant untilDate = Instant.now();
        Instant sinceDate = untilDate.minus(90, ChronoUnit.DAYS);

        LocalDate specFrom = LocalDate.ofInstant(sinceDate, zoneId);
        LocalDate specTo = LocalDate.ofInstant(untilDate, zoneId);

        var depotsResponse = helixcoreClientFactory.get(TEST_INTEGRATION_KEY).getChangeLists(specFrom, specTo, sinceDate, untilDate, 100000);
        List<HelixCoreChangeList> changelists = depotsResponse.collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(changelists);
        assertThat(changelists).isNotNull();
        testAdditions(changelists);
    }

    private void testAdditions(List<HelixCoreChangeList> changelists) {
        changelists.forEach(changelist -> {
            List<HelixCoreFile> files = changelist.getFiles();
            if (CollectionUtils.isNotEmpty(files)) {
                files
                        .forEach(file -> {
                            int additions = file.getAdditions();
                            assertThat(additions).isGreaterThanOrEqualTo(0);
                        });
            }

        });
    }


}
