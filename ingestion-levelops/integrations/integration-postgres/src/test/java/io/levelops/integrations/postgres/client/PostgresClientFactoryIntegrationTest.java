package io.levelops.integrations.postgres.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

public class PostgresClientFactoryIntegrationTest {
    private static final String QUERY = "select * from foo.policies";
    private PostgresClientFactory clientFactory;
    private static final IntegrationKey KEY = IntegrationKey.builder().tenantId("coke").integrationId("postgres1").build();

    @Before
    public void setUp() throws Exception {
        String dbServer = System.getenv("POSTGRES-DB-SERVER");
        String userName = System.getenv("POSTGRES-USERNAME");
        String password = System.getenv("POSTGRES-PASSWORD");
        String dbName = System.getenv("POSTGRES-DB-NAME");

        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .dbAuth("coke", "postgres1", "postgres", "http://levelops.atlassian.net", null, dbServer, userName, password, dbName)
                .build());

        clientFactory = PostgresClientFactory.builder()
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void testDbQuery2() throws PostgresClientException, SQLException {
        PostgresClient client = clientFactory.get(KEY);
        try(Stream<PostgresClient.Row> s=client.executeQueryStreamResults(QUERY)) {
            // stream operation
            s.peek(t -> System.out.println("new row"))
                    .forEach(System.out::println);
        }
    }

    @Test
    public void testDbQuery3() throws PostgresClientException, JsonProcessingException {
        PostgresClient client = clientFactory.get(KEY);
        client.executeQuery(QUERY)
                .stream()
                .peek(t -> System.out.println("new row"))
                .forEach(System.out::println);

        List<PostgresClient.Row> data = client.executeQuery("select * from foo.policies");
        ObjectMapper objectMapper = new ObjectMapper();
        String result = objectMapper.writeValueAsString(data);
        System.out.println(result);
    }
}