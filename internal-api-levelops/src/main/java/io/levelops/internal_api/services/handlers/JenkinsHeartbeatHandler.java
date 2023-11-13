package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.commons.generic.models.HeartbeatRequest;
import io.levelops.commons.generic.models.HeartbeatResponse;
import io.levelops.events.models.EventsClientException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class JenkinsHeartbeatHandler implements GenericRequestHandler {
    private final String requestType = "JenkinsHeartbeat";
    private final String responseType = "JenkinsHeartbeatResponse";
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_INSTANCE_NAME = "Jenkins Instance";

    @Autowired
    public JenkinsHeartbeatHandler(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, ObjectMapper objectMapper) {
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getRequestType() {
        return requestType;
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest) throws JsonProcessingException, EventsClientException {
        return handleRequest(company, genericRequest, null);
    }

    @Override
    public GenericResponse handleRequest(String company, GenericRequest genericRequest, MultipartFile zipFile)
            throws JsonProcessingException, EventsClientException {
        HeartbeatRequest heartbeatRequest = objectMapper.readValue(genericRequest.getPayload(), HeartbeatRequest.class);
        log.info("requestType : {}, company : {}, timestamp : {}, InstanceId : {}", requestType, company,
                heartbeatRequest.getTimestamp(), heartbeatRequest.getInstanceId());
        if (heartbeatRequest.getInstanceId() == null) {
            return GenericResponse.builder()
                    .responseType(responseType)
                    .payload(objectMapper.writeValueAsString(HeartbeatResponse.builder()
                            .success(false)
                            .build()))
                    .build();
        }
        try {
            Optional<CICDInstance> optCiCdInstance = ciCdInstancesDatabaseService.get(company, heartbeatRequest.getInstanceId());
            CICDInstance cicdInstance = optCiCdInstance.isEmpty() ? insertNewInstance(company, heartbeatRequest) : optCiCdInstance.get();
            boolean isUpdateSuccess = ciCdInstancesDatabaseService.update(company, cicdInstance.toBuilder()
                    .id((UUID.fromString(heartbeatRequest.getInstanceId())))
                    .lastHeartbeatAt(Instant.ofEpochSecond(heartbeatRequest.getTimestamp()))
                    .details(heartbeatRequest.getCiCdInstanceDetails())
                    .name(ObjectUtils.firstNonNull(heartbeatRequest.getCiCdInstanceDetails().getInstanceName(),
                            cicdInstance.getName(), DEFAULT_INSTANCE_NAME))
                    .url(StringUtils.isNotEmpty(heartbeatRequest.getCiCdInstanceDetails().getInstanceUrl()) ?
                            heartbeatRequest.getCiCdInstanceDetails().getInstanceUrl() : cicdInstance.getUrl())
                    .build());
            boolean hasConfigUpdate = cicdInstance.getConfigUpdatedAt() != null && hasConfigUpdate(cicdInstance.getLastHeartbeatAt()
                    , cicdInstance.getConfigUpdatedAt());
            HeartbeatResponse.HeartbeatResponseBuilder builder = HeartbeatResponse.builder()
                    .success(isUpdateSuccess);
            if (hasConfigUpdate) {
                CiCdInstanceConfig instConfig = ciCdInstancesDatabaseService.getConfig(company, heartbeatRequest.getInstanceId());
                builder.configuration(instConfig);
            }
            return GenericResponse.builder()
                    .responseType(responseType)
                    .payload(objectMapper.writeValueAsString(builder.build()))
                    .build();
        } catch (SQLException e) {
            log.error("Error updating the instance_id:{} for company :{}", heartbeatRequest.getInstanceId(), company);
            throw new RuntimeException("Error has occurred in updating the database..", e);
        }
    }

    private CICDInstance insertNewInstance(String company, HeartbeatRequest heartbeatRequest) throws SQLException {
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(UUID.fromString(heartbeatRequest.getInstanceId()))
                .lastHeartbeatAt(Instant.ofEpochMilli(heartbeatRequest.getTimestamp()))
                .name(MoreObjects.firstNonNull(heartbeatRequest.getCiCdInstanceDetails().getInstanceName(), DEFAULT_INSTANCE_NAME))
                .url(heartbeatRequest.getCiCdInstanceDetails().getInstanceUrl())
                .details(heartbeatRequest.getCiCdInstanceDetails())
                .type(CICD_TYPE.jenkins.name())
                .build();
        ciCdInstancesDatabaseService.insert(company, cicdInstance);
        return cicdInstance;
    }

    private boolean hasConfigUpdate(Instant timestampFromRequest, Instant configUpdatedAt) {
        Timestamp heartbeatTimestamp = Timestamp.from(timestampFromRequest);
        Timestamp configTimestamp = Timestamp.from(configUpdatedAt);
        return configTimestamp.compareTo(heartbeatTimestamp) > 0;
    }
}
