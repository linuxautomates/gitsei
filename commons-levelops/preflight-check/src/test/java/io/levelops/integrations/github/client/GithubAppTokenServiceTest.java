package io.levelops.integrations.github.client;

import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubAppTokenServiceTest {

    @Test
    public void generateGithubAppJwtToken() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String pem = ResourceUtils.getResourceAsString("github/jwt/private_key.pem");
        String token = GithubAppTokenService.generateGithubAppJwtToken(pem, "112233", Instant.ofEpochSecond(1659460972));
        System.out.println(token);
        assertThat(token).isEqualTo("eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiIxMTIyMzMiLCJleHAiOjE2NTk0NjE1MTIsImlhdCI6MTY1OTQ2MDkxMn0.UAGnOf1UVjoXS5hz3IAIcvSE_aBnb1sQUnpvq0z3fLnlp_bqfPP01DLzg5DC_rr2OAHz8EmZHr2wisGlIaf06grZSOtzyOIezCVHBuCmJi83R-8eFquj7ZdNrO5Lzl_hsVmg77PN-9QSbfY6ruCmvNXkMZArvI3hRIaIPXRxKub-wspD6MLQOWrXHRs4R6KA8jMA4fU7k4KzjPCIN7oD-5viRCD5v98uAphP9g9ffSNWFL3ZKFPlIvZqkH9j5569NU2jEdpzux1ASAfia_2RhSiZ_sgrQ_FjAuShfIS19vqBUupeN3NO9MtEfhwIxcwJqR-RvmL5bNTtO_6UiLNsTw");
    }

}