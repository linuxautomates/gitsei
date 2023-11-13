package io.levelops.repomapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.services.ControlPlaneService;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.postgresql.PGProperty;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AutoRepoMappingServiceIntegrationTest {
    private static String DATABASE_IP = System.getenv("DB_IP");
    private static String DATABASE_USERNAME =  System.getenv("DB_USERNAME");
    private static String DATABASE_PASSWORD =  System.getenv("DB_PASSWORD");
    private static String SSL_MODE = "require";
    private static String clientCert =  System.getenv("CLIENT_CERT_PATH");
    private static String clientKey =  System.getenv("CLIENT_KEY_PATH");
    private static String clientKeyPassword = "";
    private static String serverCert =  System.getenv("SERVER_CERT_PATH");
    private static Long DB_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    private static int maxPoolSize = 2;
    private static Long DB_LEAK_DETECTION_THRESHOLD = TimeUnit.MINUTES.toMillis(2);

    private DataSource createDataSource() {
        String url = "jdbc:postgresql://" + DATABASE_IP + "/postgres?";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(DATABASE_USERNAME);
        config.setPassword(DATABASE_PASSWORD);
        config.addDataSourceProperty(PGProperty.SSL_MODE.getName(), SSL_MODE);
        config.addDataSourceProperty(PGProperty.SSL_CERT.getName(), clientCert);
        config.addDataSourceProperty(PGProperty.SSL_KEY.getName(), clientKey);
        config.addDataSourceProperty(PGProperty.SSL_PASSWORD.getName(), clientKeyPassword);
        config.addDataSourceProperty(PGProperty.SSL_ROOT_CERT.getName(), serverCert);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(DB_CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(DB_LEAK_DETECTION_THRESHOLD);
        return new HikariDataSource(config);
    }

    @Test
    public void test() throws SQLException, IngestionServiceException, InterruptedException, TimeoutException {
        DataSource dataSource = createDataSource();
        ObjectMapper objectMapper = new ObjectMapper();
        OkHttpClient okHttpClient = new OkHttpClient();
        OrgVersionsDatabaseService orgVersionsDatabaseService = new OrgVersionsDatabaseService(dataSource);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        OrgUsersDatabaseService orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, objectMapper, orgVersionsDatabaseService, userIdentityService);
        ControlPlaneService controlPlaneService = new ControlPlaneService(okHttpClient, objectMapper, "http://localhost:8081/", true);
        InventoryService inventoryService = new InventoryServiceImpl("http://localhost:8080/", okHttpClient, objectMapper);
        AutoRepoMappingService autoRepoMappingService = new AutoRepoMappingService(
                orgUsersDatabaseService, controlPlaneService, inventoryService, objectMapper
        );
        var scmRepoMappingResult = autoRepoMappingService.createAndWaitForRepoMappingJob(IntegrationKey.builder()
                .integrationId("11")
                .tenantId("sidofficial")
                .build(), 120);
        System.out.println(scmRepoMappingResult);
    }

}