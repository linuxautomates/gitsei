package io.levelops.auth.controllers;

import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;

import org.assertj.core.api.Assertions;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unused")
public class MFAControllerTest {
    
    private static UserService userService;
    private static RedisConnectionFactory redisConnectionFactory;
    private static SecretsManagerServiceClient secretsManagerServiceClient;
    private static RedisConnection redisConnection;
    private static SecretGenerator secretGenerator;
    private static TenantConfigService tenantConfigService;
    private static CacheManager cacheManager;

    @Captor
    private ArgumentCaptor<byte[]> secretCaptor;

    @BeforeClass
    public static void setup(){
        // MockitoAnnotations.initMocks(MFAControllerTest.class);
        userService = Mockito.mock(UserService.class);
        redisConnectionFactory = Mockito.mock(RedisConnectionFactory.class);
        secretsManagerServiceClient = Mockito.mock(SecretsManagerServiceClient.class);
        redisConnection = Mockito.mock(RedisConnection.class);
        secretGenerator = Mockito.mock(SecretGenerator.class);
        cacheManager = Mockito.mock(CacheManager.class);
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    public void test() throws SQLException, NoSuchAlgorithmException{
        var secret = "3TCLCLSJBDVURDNVKJBU7ABUA2WQCM5P";
        when(userService.getByEmail(anyString(), anyString())).thenReturn(Optional.of(User.builder().email("email").mfaEnabled(false).build()));
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.setEx(eq("company_username_enrollment_tmp".getBytes()), anyLong(), secretCaptor.capture())).thenReturn(true);
        when(redisConnection.get(any(byte[].class))).thenReturn(secret.getBytes());
        when(secretGenerator.generate()).thenReturn(secret);
        var controller = new MFAController(redisConnectionFactory, userService, secretsManagerServiceClient, secretGenerator, cacheManager);
        var enrollment = controller.get("company", "username");
        var count = 0;
        while(!enrollment.hasResult() && count < 10){
            try {
                Thread.sleep(300);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Cache cache = Mockito.mock(Cache.class);
        // when(cacheManager.getCache(anyString())).thenReturn(cache);
        // when(cache.evict(any())).thenReturn();
        var results = (ResponseEntity<MultiValueMap>) enrollment.getResult();
        var mfaCode = (Map<String, Object>) ((List<HttpEntity>) results.getBody().get("code")).get(0).getBody();

        Assertions.assertThat(mfaCode).containsAllEntriesOf(Map.of("secret", secret, "username", "username", "issuer", "company@levelops", "algorithm", "SHA1", "digits", 6, "period", 30));

        var totp = new Totp((String) mfaCode.get("secret"));
        
        var code = totp.now();
        var response = controller.enroll("company", "username", MFAController.EnrollmentVerification.builder().otp(code).build());
        count = 0;
        while(!response.hasResult() && count < 10){
            try {
                Thread.sleep(300);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assertions.assertThat(((ResponseEntity<Map<String, String>>)response.getResult()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // @Test
    public void testQR(){
        var secret = "VGEJQUOTMXMHEVH2R7MQA2U3JDYX6DDE";

        QrData data = new QrData.Builder()
            .label("test")
            .secret(secret)
            .issuer("test" + "@levelops")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
        QrGenerator qrGenerator = new ZxingPngQrGenerator();

        byte[] imageData;
        try {
            imageData = qrGenerator.generate(data);
            Files.copy(new ByteArrayInputStream(imageData), Path.of("/tmp/1.png"));
        } catch (QrGenerationException | IOException e) {
            e.printStackTrace();
        }
        
        // var totp1 = new Totp(secret);
        // var code1 = totp1.now();
        // var time = Instant.now().getEpochSecond();
    }

    @Test
    public void testSecretsKey(){
        var result = MFAController.getSecretsManagerKey("test", "user1@test.test");
        Assertions.assertThat(result).isEqualTo("test_user1_test_test_mfa");

        result = MFAController.getSecretsManagerKey("test", "user.1@test.test");
        Assertions.assertThat(result).isEqualTo("test_user_1_test_test_mfa");

        result = MFAController.getSecretsManagerKey("test", "user.1+2@test.test");
        Assertions.assertThat(result).isEqualTo("test_user_1_2_test_test_mfa");

        result = MFAController.getSecretsManagerKey("test", "user.1+2[@test.test");
        Assertions.assertThat(result).isEqualTo("test_user_1_2__test_test_mfa");
    }
}
