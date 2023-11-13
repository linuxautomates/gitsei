package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import io.levelops.io.levelops.scm_repo_mapping.models.ScmRepoMappingResponse;
import io.levelops.repomapping.AutoRepoMappingService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Objects;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/internal/v1/tenants/{tenant_id}/scm_repo_mapping")
@Log4j2
public class ScmRepoMappingController {
    private final AutoRepoMappingService autoRepoMappingService;
    private final ObjectMapper objectMapper;
    private final RedisConnectionFactory redisConnectionFactory;
    private final int jobIdCacheTtlSeconds;

    @Autowired
    public ScmRepoMappingController(
            AutoRepoMappingService autoRepoMappingService,
            ObjectMapper objectMapper,
            RedisConnectionFactory redisConnectionFactory,
            @Value("${JOB_ID_CACHE_TTL_SECONDS:1800}") int jobIdCacheTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.redisConnectionFactory = redisConnectionFactory;
        this.autoRepoMappingService = autoRepoMappingService;
        this.jobIdCacheTtlSeconds = jobIdCacheTtlSeconds;
    }

    private String getRedisKey(String tenantId, String integrationId) {
        return String.format("%s:%s:repo_mapping", tenantId, integrationId);
    }

    @GetMapping("/repo_mapping")
    public DeferredResult<ResponseEntity<ScmRepoMappingResponse>> getRepoMapping(
            @PathVariable("tenant_id") final String tenantId,
            @RequestParam(value = "integration_id") String integrationId,
            @RequestParam(value = "there_is_no_cache", required = false, defaultValue = "false") boolean thereIsNoCache
    ) {
        Validate.notBlank(integrationId, "Integration id is required");
        return SpringUtils.deferResponse(() -> getRepoMappingInternal(tenantId, integrationId, thereIsNoCache));
    }

    @VisibleForTesting
    public ResponseEntity<ScmRepoMappingResponse> getRepoMappingInternal(String tenantId, String integrationId, boolean thereIsNoCache) {
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            String redisKey = getRedisKey(tenantId, integrationId);
            var keyExists = redis.exists(redisKey.getBytes(UTF_8));
            if (!thereIsNoCache && BooleanUtils.isTrue(keyExists)) {
                String jobId = new String(Objects.requireNonNull(redis.get(redisKey.getBytes(UTF_8))), UTF_8);
                log.info("Found job id in redis for key: {}, job id: {}", redisKey, jobId);
                Optional<ScmRepoMappingResult> result = autoRepoMappingService.getJobResultIfAvailable(jobId);
                return result.map(scmRepoMappingResult ->
                                new ResponseEntity<>(
                                        ScmRepoMappingResponse
                                                .builder()
                                                .jobId(jobId)
                                                .result(scmRepoMappingResult).build(),
                                        HttpStatus.OK))
                        .orElseGet(() ->
                                new ResponseEntity<>(
                                        ScmRepoMappingResponse
                                                .builder()
                                                .jobId(jobId)
                                                .build(),
                                        HttpStatus.ACCEPTED));
            } else {
                String newJobId = autoRepoMappingService.createRepoMappingJob(IntegrationKey.builder()
                        .integrationId(integrationId)
                        .tenantId(tenantId)
                        .build(), "");
                log.info("Job id not found in redis for key: {} - created new job: {}", redisKey, newJobId);
                if (BooleanUtils.isNotTrue(redis.setEx(redisKey.getBytes(UTF_8), jobIdCacheTtlSeconds, newJobId.getBytes(UTF_8)))) {
                    log.error("Error while setting redis job id key: {}, job id: {}", redisKey, newJobId);
                    throw new RuntimeException("Error while setting redis key");
                }
                return new ResponseEntity<>(ScmRepoMappingResponse.builder().jobId(newJobId).build(), HttpStatus.ACCEPTED);
            }
        } catch (Exception e) {
            log.error("Error while getting repo mapping job result", e);
            throw new RuntimeException("Error while getting repo mapping job result", e);
        }
    }
}

