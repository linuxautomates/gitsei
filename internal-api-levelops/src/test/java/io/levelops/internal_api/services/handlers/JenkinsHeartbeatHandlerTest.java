package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.CiCdInstanceDetails;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.HeartbeatRequest;
import io.levelops.commons.generic.models.HeartbeatResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.events.models.EventsClientException;
import io.levelops.internal_api.services.GenericRequestHandlerService;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class JenkinsHeartbeatHandlerTest {

    private static final String COMPANY = "test";
    private static final String INSTANCE_ID = "0dc0fdb1-82da-46da-b036-d4f7ba93e25d";
    private GenericRequestHandlerService genericRequestHandlerService;
    private JenkinsHeartbeatHandler jenkinsHeartbeatHandler;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private ObjectMapper objectMapper;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;

    @Before
    public void setUp() throws SQLException {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        objectMapper = DefaultObjectMapper.get();
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.ensureTableExistence(COMPANY);
        ciCdInstancesDatabaseService.insert(COMPANY, CICDInstance.builder()
                .id(UUID.fromString(INSTANCE_ID))
                .name("Jenkins Instance")
                .updatedAt(Instant.now())
                .lastHeartbeatAt(Instant.now())
                .createdAt(Instant.now())
                .build());
        jenkinsHeartbeatHandler = new JenkinsHeartbeatHandler(ciCdInstancesDatabaseService, objectMapper);
        genericRequestHandlerService = new GenericRequestHandlerService(List.of(jenkinsHeartbeatHandler));
    }

    @Test
    public void test() throws EventsClientException, BadRequestException, JsonProcessingException, SQLException {
        final Instant timestamp = Instant.now();
        HeartbeatRequest heartbeatRequestForConfigUpdate = HeartbeatRequest.builder()
                .instanceId(INSTANCE_ID)
                .timestamp(timestamp.getEpochSecond())
                .ciCdInstanceDetails(CiCdInstanceDetails.builder()
                        .jenkinsVersion("2.1")
                        .pluginVersion("2.2")
                        .build())
                .build();
        String payload = objectMapper.writeValueAsString(heartbeatRequestForConfigUpdate);
        assertThat(genericRequestHandlerService.handleRequest(COMPANY, GenericRequest.builder()
                .requestType("JenkinsHeartbeat")
                .payload(payload).build(), null)
                .getResponseType()).isEqualTo("JenkinsHeartbeatResponse");
        CICDInstance ciCdInstance = ciCdInstancesDatabaseService.get(COMPANY, INSTANCE_ID).get();
        assertThat(ciCdInstance.getId()).isEqualTo(UUID.fromString(INSTANCE_ID));
        assertThat(ciCdInstance.getDetails()).isNotNull();
        assertThat(ciCdInstance.getDetails().getJenkinsVersion()).isEqualTo("2.1");
        assertThat(ciCdInstance.getDetails().getPluginVersion()).isEqualTo("2.2");
        assertThat(ciCdInstance.getLastHeartbeatAt().getEpochSecond()).isEqualTo(timestamp.getEpochSecond());
        var heartbeatResponseBuilder = HeartbeatResponse.builder()
                .success(true)
                .configuration(CiCdInstanceConfig.builder().build())
                ;
        assertThat(genericRequestHandlerService.handleRequest(COMPANY, GenericRequest.builder()
                .requestType("JenkinsHeartbeat")
                .payload(payload).build(), null)
                .getPayload()).isEqualTo(objectMapper.writeValueAsString(heartbeatResponseBuilder.build()));
    }
}
