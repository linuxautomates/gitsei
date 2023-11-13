package io.levelops.auth.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.SecretsManagerServiceClient.KeyValue;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.web.util.SpringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RestController
@RequestMapping("/v1/mfa")
@SuppressWarnings("unused")
public class MFAController {
    private static final String MFA_ENROLLMENT_WINDOW_EXPIRY_CONFIG_NAME = "MFA_ENROLLMENT_WINDOW";
    private static final String MFA_ENFORCED_CONFIG_NAME = "MFA_ENFORCED";
    private static final String MFA_SECRET_ID = "%s_%s_mfa";

    private final SecretGenerator secretGenerator;
    private final SecretsManagerServiceClient secretsManagerServiceClient;
    private final RedisConnectionFactory redisFactory;
    private final UserService userService;
    private final CacheManager cacheManager;

    @Autowired
    public MFAController(
            final RedisConnectionFactory redisFactory,
            final UserService userService,
            final SecretsManagerServiceClient secretsManagerServiceClient,
            final SecretGenerator secretGenerator,
            final CacheManager cacheManager){
        this.redisFactory = redisFactory;
        this.userService = userService;
        this.secretsManagerServiceClient = secretsManagerServiceClient;
        this.secretGenerator = secretGenerator;
        this.cacheManager = cacheManager;
    }

    @Value
    @Builder(toBuilder = true)
    @AllArgsConstructor
    @JsonDeserialize(builder = EnrollmentVerification.EnrollmentVerificationBuilder.class)
    public static final class EnrollmentVerification{
        @JsonProperty("otp")
        String otp;
    }
    
    @GetMapping(path = "/enroll", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeferredResult<ResponseEntity<MultiValueMap<String, HttpEntity<?>>>> get(@SessionAttribute("company") String company, @SessionAttribute("session_user") String username){
        return SpringUtils.deferResponse(() -> {
            log.info("[{}] MFA enrollment for {}", company, username);
            // check if the user is not already enrolled in MFA
            var user = userService.getByEmail(company, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User '" + username + "'not found"));
            if(user.getMfaEnabled()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            // generate secret
            var secret = secretGenerator.generate();
            try(var redis = redisFactory.getConnection()){
                var enrollmentKey = getTempMFAUserEnrolledKey(company, username);
                if(!redis.setEx(enrollmentKey.getBytes(), Duration.ofHours(6L).getSeconds(), secret.getBytes())){
                    log.error("[{}] Unable to set the tmp mfa enrollment code for the user '{}'", company, username);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, please try again in a few minutes or contact support.");
                }
            } catch(Exception e){
                log.error("[{}] Unable to set the tmp mfa enrollment code for the user '{}'", company, username, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong, please try again in a few minutes or contact support.");
            }
            var builder = new MultipartBodyBuilder();
            builder.part("code", Map.of("secret",secret, "username", username, "issuer", company + "@levelops", "algorithm", "SHA1", "digits", 6, "period", 30)).contentType(MediaType.APPLICATION_JSON);
            // generate the QRCode
            QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(company + "@levelops")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
            QrGenerator qrGenerator = new ZxingPngQrGenerator();
            try {
                byte[] image = qrGenerator.generate(data);
                byte[] base64 = Base64.getEncoder().encode(image);
                builder.part("qrcode", base64).contentType(MediaType.TEXT_PLAIN).filename("QRCode.png");
                return ResponseEntity.ok(builder.build());
            } catch (QrGenerationException e) {
                log.error("[{}] Unable to generate the enrollment QRCode", company, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        });
    }
    
    @PostMapping(path="/enroll", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, String>>> enroll(@SessionAttribute("company") String company, @SessionAttribute("session_user") String username, @RequestBody EnrollmentVerification verification){
        return SpringUtils.deferResponse(() -> {
            log.info("[{}] completing enrollment for '{}'", company, username);
            log.debug("[{} - {}] code: {}", company,username, verification.getOtp());
            var enrollmentKey = getTempMFAUserEnrolledKey(company, username);
            try(var redis = redisFactory.getConnection()){
                var secretBytes = redis.get(enrollmentKey.getBytes());
                if(secretBytes == null){
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unable to validate the OTP code"));
                }
                var secret = new String(secretBytes);
                TimeProvider timeProvider = new SystemTimeProvider();
                CodeGenerator codeGenerator = new DefaultCodeGenerator();
                CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
                if(verifier.isValidCode(secret, verification.getOtp())){
                    // save it to secrets manager and update user config
                    try {
                        secretsManagerServiceClient.storeKeyValue(company, SecretsManagerServiceClient.DEFAULT_CONFIG_ID, KeyValue.builder().key(getSecretsManagerKey(company, username)).value(secret).build());
                        var user = userService.getByEmail(company, username).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the MFA enrollment, please try again."));
                        userService.update(company, user.toBuilder().mfaEnabled(true).mfaEnrollmentEndAt(Instant.parse("1970-01-01T12:00:00-00:00")).build());
                        redis.del(enrollmentKey.getBytes());
                        // cacheManager.getCache("auth").evict(keyGeneratorForUserMFAEnrolled(company, username));
                        return ResponseEntity.ok(Map.of("status", "ok"));
                    } catch (ResponseStatusException | SQLException | SecretsManagerServiceClientException e) {
                        log.error("[{}] Unable to complete MFA enrollment for user '{}'", company, username, e);
                    }
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Unable to validate the OTP code"));
            }
        });
    }

    // @Cacheable(value = "auth", key = "io.levelops.auth.controllers.MFAController.keyGeneratorForUserMFAEnrolled(#company, #username)")
    public static String getMFASecret(final SecretsManagerServiceClient secretsService, final String company, final String username){
        var configId = SecretsManagerServiceClient.DEFAULT_CONFIG_ID; // when more than 1 secret manager supported, we will need to read this from some other place
        var userMfaSecretId = getSecretsManagerKey(company, username);
        try {
            var secret = secretsService.getKeyValue(company, configId, userMfaSecretId);
            return secret != null ? secret.getValue() : null;
        }
        catch (SecretsManagerServiceClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if(rootCause instanceof HttpException && ((HttpException) rootCause).getCode() == 404){
                log.info("[{}] mfa secret not found for {}, returning null", company, username);
                return null;
            }
            log.error("[{}] Unable to retreive the mfa secret for the user '{}'. returning a random secret to block access", company, username, e);
            return "sec_ret";
        }
    }

    // @Cacheable(value = "auth", key = "io.levelops.auth.controllers.MFAController.keyGeneratorForUserMFAEnrolled(#company, #username)")
    public static Boolean isMFAEnrolled(final SecretsManagerServiceClient secretsService, final String company, final String username){
        var secret = getMFASecret(secretsService, company, username);
        var mfaEnabled = StringUtils.isNotBlank(secret);
        log.debug("[{} - {}] mfa enabled? {}", company, username, mfaEnabled);
        return mfaEnabled;
    }

    // @Cacheable(value = "auth", key = "io.levelops.auth.auth.service.AuthDetailsService.keyGeneratorForGlobalEnrollmentWindowExpiration(#company)")
    public static Optional<Instant> getGlobalEnrollmentWindowExpiration(final TenantConfigService configService, final String company){
        try {
            return configService.listByFilter(company, MFA_ENROLLMENT_WINDOW_EXPIRY_CONFIG_NAME, 0, 1)
                                    .getRecords()
                                    .stream()
                                    .findFirst()
                                    .map(config -> Instant.ofEpochSecond(Long.valueOf(config.getValue())));
        } catch (SQLException e) {
            log.error("[{}] Unable to get the global expiry window. will use the current date", company, e);
            return Optional.of(Instant.now());
        }
    }

    // @Cacheable(value = "auth", key = "io.levelops.auth.controllers.MFAController.keyGeneratorForGlobalMFAEnforced(#company)")
    public static Boolean isMFAEnforced(final TenantConfigService configService, final String company){
        try {
            return configService.listByFilter(company, MFA_ENFORCED_CONFIG_NAME, 0, 1)
                                    .getRecords()
                                    .stream()
                                    .findFirst()
                                    .map(config -> Strings.isNotBlank(config.getValue()) && Long.valueOf(config.getValue()) > 0).orElse(false);
        } catch (SQLException e) {
            log.error("[{}] Unable to get the global mfs enforcement config. will choose the more secure response", company, e);
            return true;
        }
    }

    public static String getTempMFAUserEnrolledKey(final String company, final String username){
        return String.format("%s_%s_enrollment_tmp", company, username);
    }

    public static String keyGeneratorForGlobalEnrollmentWindowExpiration(final String company){
        return String.format("%s_cache_global_enrollment_window_expiration", company);
    }

    public static String keyGeneratorForGlobalMFAEnforced(final String company){
        return String.format("%s_cache_global_mfa_enforced", company);
    }

    public static String keyGeneratorForUserMFAEnrolled(final String company, final String username){
        return String.format("%s_cache_%s_mfa_enrolled", company, username);
    }

    public static String getSecretsManagerKey(final String company, final String username) {
        return String.format(MFA_SECRET_ID, company, username.replaceAll("[^A-Za-z0-9]", "_"));//"@", "_").replaceAll("\\.", "_"));
    }

    public static Boolean deleteMFASecret(final SecretsManagerServiceClient secretsManagerServiceClient, final String company, final String username) {
        try{
            var key = getSecretsManagerKey(company, username);
            secretsManagerServiceClient.deleteKeyValue(company, SecretsManagerServiceClient.DEFAULT_CONFIG_ID, key);
            return true;
        } catch (SecretsManagerServiceClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if(rootCause instanceof HttpException && ((HttpException) rootCause).getCode() == 404){
                log.warn("[{}] mfa enrollment not found for {}, nothing to do", company, username);
            }
            log.error("[{}] Error deleting the mfa secret for the user '{}'", company, username, e);
        }
        return false;
    }
}
