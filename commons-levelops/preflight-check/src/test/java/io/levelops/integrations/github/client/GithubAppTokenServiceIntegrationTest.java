package io.levelops.integrations.github.client;

import com.amazonaws.util.IOUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;

public class GithubAppTokenServiceIntegrationTest {

    @Test
    public void generateAccessToken() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        GithubAppTokenService tokenService = new GithubAppTokenService(new OkHttpClient(), DefaultObjectMapper.get(), null, null);

        String githubApiUrl = "https://api.github.com";
        String appId = "308821";
        String installationId = "35556693";
        String pemPrivateKey = IOUtils.toString(new FileInputStream(System.getenv("GITHUB_PEM_FILE_PATH")));

        String accessToken = tokenService.generateAccessToken(githubApiUrl, appId, installationId, pemPrivateKey);
        String jwtToken = GithubAppTokenService.generateGithubAppJwtToken(pemPrivateKey, appId, Instant.now());

        System.out.println(">>>> Access Token");
        System.out.println(accessToken);
        System.out.println("<<<<");

        System.out.println(">>>> JWT Token");
        System.out.println(jwtToken);
        System.out.println("<<<<");
    }

    @Test
    public void testSeiApp() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String appId = "364246";
        String pemFilePath = "/Users/siddharthbidasaria/Downloads/harness-sei-dev-github-app.2023-07-20.private-key.pem";
        String pemPrivateKey = IOUtils.toString(new FileInputStream(pemFilePath));
        System.out.println(pemPrivateKey);

        GithubAppTokenService tokenService = new GithubAppTokenService(new OkHttpClient(), DefaultObjectMapper.get(), appId, pemPrivateKey);
        tokenService.generateAccessTokenForSeiApp("https://api.github.com", "39864632");
    }
}
