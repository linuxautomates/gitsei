package io.levelops.auth.utils;

import com.amazonaws.services.acmpca.model.InvalidRequestException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.Claim;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.levelops.auth.auth.authobject.ExtendedUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Log4j2
public class JwtTokenUtil implements Serializable {
    private static final long serialVersionUID = -2550185165626007488L;
    private static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60; // 5 hours
    private static final long JWT_TOKEN_SHORT_VALIDITY = 5 * 60; // 5 mins
    private static final long JWT_TOKEN_ONBOARDING_VALIDITY = 24 * 60 * 60; // 5 mins

    private static final String COMPANY = "company";
    private static final String USER_TYPE = "user_type";
    private static final String MFA_ENROLLMENT = "mfa_enrollment_only";
    private static final String ONBOARDING_ONLY = "onboarding_only";

    private final Key key;

    public JwtTokenUtil(String jwtSigningKey) {
        key = new SecretKeySpec(jwtSigningKey.getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    public String getUsernameFromClaims(Claims claims) {
        return claims.getSubject();
    }

    public String getCompanyFromClaims(Claims claims) {
        return claims.get(COMPANY, String.class);
    }

    public Boolean isTokenForMFAEnrollment(Claims claims) {
        var value = claims.get(MFA_ENROLLMENT, Boolean.class);
        return value != null ? value : false;
    }

    private Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public long getJwtTokenValiditySeconds() {
        return JWT_TOKEN_VALIDITY;
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(ExtendedUser userDetails, String company) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(COMPANY, company);
        claims.put(USER_TYPE, userDetails.getUserType());
        return doGenerateToken(claims, userDetails.getUsername(), getJwtTokenValiditySeconds());
    }

    public String generateTokenForOnboarding(ExtendedUser userDetails, String company) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(COMPANY, company);
        claims.put(USER_TYPE, userDetails.getUserType());
        claims.put(ONBOARDING_ONLY, true);
        return doGenerateToken(claims, userDetails.getUsername(), JWT_TOKEN_ONBOARDING_VALIDITY);
    }

    public String generateTokenForMFAEnrollment(ExtendedUser userDetails, String company) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(COMPANY, company);
        claims.put(USER_TYPE, userDetails.getUserType());
        claims.put(MFA_ENROLLMENT, true);
        return doGenerateToken(claims, userDetails.getUsername(), getJwtTokenValiditySeconds());
    }

    public String generateShortTermToken(ExtendedUser userDetails, String company) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(COMPANY, company);
        claims.put(USER_TYPE, userDetails.getUserType());
        return doGenerateToken(claims, userDetails.getUsername(), JWT_TOKEN_SHORT_VALIDITY);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject, Long validityDuration) {
        String tokenGenerated = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validityDuration * 1000))
                .signWith(key).compact();
        return tokenGenerated;
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token);
    }

    /**
     * Verifies the validity of a JWT (JSON Web Token) by verifying it against the provided secret key.
     * @param jwtToken The JWT token to be verified.
     * @param secret he secret key used for verifying the JWT token's signature.
     * @return A {@code Map<String, Claim>} containing the claims extracted from the JWT token
     *         if it is valid; {@code null} if the token is invalid or if an error occurs during verification.
     * @throws IllegalArgumentException If the JWT token is malformed or if the secret key is invalid.
     * @throws SignatureVerificationException If the JWT token's signature does not match the expected value.
     * @throws JWTDecodeException If there is an error decoding the JWT token.
     */
    public Map<String, Claim> verifyJWTToken(String jwtToken, String secret) {

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).withIssuer("Harness Inc").build();
            verifier.verify(jwtToken);
            return JWT.decode(jwtToken).getClaims();
        } catch (JWTDecodeException | SignatureVerificationException | InvalidClaimException |
                 UnsupportedEncodingException e) {
            log.error("Error while verifying jwt token: Invalid JWTToken received, failed to decode the token", e);
            throw new InvalidRequestException("Error while verifying jwt token: Invalid JWTToken received, failed to decode the token");
        }
    }
}
