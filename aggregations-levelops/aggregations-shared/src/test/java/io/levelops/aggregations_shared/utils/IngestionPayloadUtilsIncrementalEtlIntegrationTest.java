package io.levelops.aggregations_shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.GcsStorageService;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.postgresql.PGProperty;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class IngestionPayloadUtilsIncrementalEtlIntegrationTest {
    private static String DATABASE_IP = "104.155.177.90";
    private static String DATABASE_USERNAME = "";
    private static String DATABASE_PASSWORD = "";
    private static String SSL_MODE = "require";
    private static String clientCert = "";
    private static String clientKey = "";
    private static String clientKeyPassword = "";
    private static String serverCert = "";
    private static Long DB_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    ;
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
    public void test() throws IOException, SQLException, IngestionServiceException {
        var dataSource = createDataSource();
        ObjectMapper mapper = DefaultObjectMapper.get();

        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        ControlPlaneService controlPlaneService = new ControlPlaneService(okHttpClient, objectMapper, "http://localhost:8081", true);
        JobDefinitionDatabaseService jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        JobInstanceDatabaseService jobInstanceDatabaseService = new JobInstanceDatabaseService(mapper, dataSource, jobDefinitionDatabaseService);
        GcsStorageService gcsStorageService = new GcsStorageService("etl-payload", "");
        IngestionResultPayloadUtils ingestionResultPayloadUtils = new IngestionResultPayloadUtils(
                mapper, controlPlaneService, jobInstanceDatabaseService, jobDefinitionDatabaseService, gcsStorageService
        );
        DbJobDefinition jobDefinition = jobDefinitionDatabaseService.get(UUID.fromString("1c0609ac-af52-48a9-b21f-6ba00d16ddf7")).get();
        var payload = ingestionResultPayloadUtils.generatePayloadForJobDefinition(jobDefinition, false);
        System.out.println("payload gcs records size: " + payload.getGcsRecords().size());
    }
}
