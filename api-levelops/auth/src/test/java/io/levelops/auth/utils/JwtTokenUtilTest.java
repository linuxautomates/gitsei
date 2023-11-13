package io.levelops.auth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

public class JwtTokenUtilTest {

  private Algorithm algorithm;
  private JwtTokenUtil jwtTokenUtil;
  private String secret = "HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU=";

  @Before
  public void setup() throws UnsupportedEncodingException {
    algorithm = Algorithm.HMAC256(secret);
    jwtTokenUtil = new JwtTokenUtil(secret);
  }

  @Test
  public void testVerifyValidJWTToken() {
    String jwtToken = JWT.create()
                          .withClaim("accountId", "kmpySmUISimoRrJL6NL73w")
                          .withClaim("principal", "lv0euRhKRCyiXWzS7pOg6g")
                          .withClaim("principalType", "USER")
                          .withClaim("email", "ashish@harness.io")
                          .withIssuer("Harness Inc")
                          .withExpiresAt(new Date(System.currentTimeMillis()+5000))
                          .sign(algorithm);

    Map<String, Claim> claims = jwtTokenUtil.verifyJWTToken(jwtToken, secret);

    Assertions.assertThat(claims.get("accountId").asString())
        .isEqualTo("kmpySmUISimoRrJL6NL73w");
    Assertions.assertThat(claims.get("principal").asString())
        .isEqualTo("lv0euRhKRCyiXWzS7pOg6g");
    Assertions.assertThat(claims.get("principalType").asString())
        .isEqualTo("USER");
    Assertions.assertThat(claims.get("email").asString())
        .isEqualTo("ashish@harness.io");

    try {
      String incorrectsecret = "HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhmmn0K4Ttr0uoOxSsEVYNrUU=";
      claims = jwtTokenUtil.verifyJWTToken(jwtToken, incorrectsecret);
    } catch (Exception e) {
      Assertions.assertThat(e.getMessage()).contains("Error while verifying jwt token");
    }
  }
}
