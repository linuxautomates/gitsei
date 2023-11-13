package io.levelops.auth.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.config.EntitlementAPIMappingConfig;
import io.levelops.auth.httpmodels.EntitlementDetails;
import io.levelops.auth.httpmodels.Entitlements;
import io.levelops.auth.httpmodels.EntitlementsCategory;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ApiKeyToken;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static io.levelops.commons.client.ClientConstants.APIKEY;
import static io.levelops.commons.client.ClientConstants.AUTHORIZATION;
import static io.levelops.commons.client.ClientConstants.BEARER;

@Log4j2
public class AuthRequestFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;
    private final AuthDetailsService authDetailsService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Boolean enforceApiRestrictions;
    private final Auth auth;

    public AuthRequestFilter(JwtTokenUtil jwtTokenUtil, AuthDetailsService authDetailsService, ObjectMapper mapper,
                             final RedisConnectionFactory redisConnectionFactory, final Boolean enforceApiRestrictions, Auth auth) {
        this.objectMapper = mapper;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authDetailsService = authDetailsService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.enforceApiRestrictions = enforceApiRestrictions;
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (auth.isLegacy()) {
            final String requestTokenHeader = request.getHeader("Authorization");
            final String dashboardId = request.getHeader("Dashboard-Id");
            final String authParam = request.getParameter("Authorization");
            final String apiKey = request.getParameter("Apikey");
            final String uri = request.getRequestURI();
            final String method = request.getMethod();

            String username = null;
            String company = null;
            String userType = null;
            Boolean successfulAuth = false;
            Map<String, List<String>> scopes = null;
            List<Entitlements> entitlements = null;
            boolean allowedApiRequest = true;

            //not returning early in any of the ifs because we want the filter to attempt to execute the request
            if (requestTokenHeader != null || apiKey != null || authParam != null) {
                String[] tokens = getTokens(requestTokenHeader, apiKey, authParam);
                if (tokens.length == 2) {
                    //param or bearer have the same behavior
                    if (BEARER.equalsIgnoreCase(tokens[0]) || AUTHORIZATION.equalsIgnoreCase(tokens[0])) {
                        String jwtToken = tokens[1];
                        try {
                            Claims claims = jwtTokenUtil.getAllClaimsFromToken(jwtToken);
                            company = jwtTokenUtil.getCompanyFromClaims(claims);
                            username = jwtTokenUtil.getUsernameFromClaims(claims);
                            var mfaEnrollment = jwtTokenUtil.isTokenForMFAEnrollment(claims);
                            ExtendedUser user = authDetailsService.loadUserByUsernameAndOrg(username, company);
                            userType = user.getUserType();
                            scopes = user.getScopes();
                            successfulAuth = jwtTokenUtil.validateToken(jwtToken);
                            // check if user is not enrolling in MFA
                            try (var redis = redisConnectionFactory.getConnection()) {
                                // only successfull if:
                                //  - there is no enrollment flow ongoing
                                //  - the user is not required to enrolled
                                //  - or if the user is enrolling or required to enrolled and the url being accessed is the enrollment one
                                successfulAuth &= (!redis.exists(String.format("%s_%S_enrollment_tmp", company, username).getBytes()) && !mfaEnrollment)
                                        || request.getRequestURI().equals("/v1/mfa/enroll");
                                if (!successfulAuth) {
                                    log.info("User is in MFA enrolling and is trying to access a different URL. Access denied");
                                }
                            }

                        } catch (IllegalArgumentException e) {
                            log.error("Unable to get JWT Token, company " + company + " username " + username, e);
                        } catch (ExpiredJwtException e) {
                            log.error("JWT Token has expired, company " + company + " username " + username, e);
                        } catch (UsernameNotFoundException | SQLException e) {
                            log.error("Failed to get valid user, company " + company + " username " + username, e);
                        }
                    } else if (APIKEY.equalsIgnoreCase(tokens[0])) {
                        try {
                            ApiKeyToken token = objectMapper.readValue(
                                    Base64.getMimeDecoder().decode(tokens[1]),
                                    ApiKeyToken.class);
                            company = token.getCompany();
                            username = token.getId();
                            //the validate throws exception on failure which will prevent successfulauth = true
                            userType = authDetailsService.validateKeyAndGetRole(company, username, token.getKey()).toString();
                            //scopes = authDetailsService.loadUserByUsernameAndOrg(username,company).getScopes();
                            successfulAuth = true;

                        } catch (UsernameNotFoundException | IllegalAccessException | ExecutionException e) {
                            //I think the stack trace is not usefull for this case as it fills up the logs and no one is looking at it at the moment
                            log.error("[{} - {}] {} - {}", company, request.getRemoteAddr(), request.getRequestURL(), e.getMessage());
                        } catch (BadCredentialsException | SQLException e) {
                            log.error("[{}] Failed authentication.", company, e);
                        } catch (IllegalArgumentException e) {
                            log.error("[{}] Failed to decode token: method={} uri={} token='{}'", company, method, uri, tokens[1]);
                            throw e;
                        }
                    }
                }
            }

            if (enforceApiRestrictions && company != null && !company.equals("_levelops")) {
                try {
                    entitlements = authDetailsService.getCompanyEntitlements(company);
                    if (!entitlements.contains(Entitlements.ALL_FEATURES)) {
                        allowedApiRequest = false;
                        allowedApiRequest = checkAccess(entitlements, uri, method);
                    }
                } catch (Exception e) {
                    allowedApiRequest = false;
                    log.error("Failed to get license for company " + company, e);
                }
            }

            //Once we get the token validate it.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null && allowedApiRequest) {
                // if token is valid configure Spring Security to manually set authentication
                if (successfulAuth) {
                    // if dashboard id is present and the dashboard is public then replace the type for public_dashboard
                    // This will limit the call to only the enabled endpoints for public dashboard
                    if (!RoleType.ADMIN.toString().equalsIgnoreCase(userType)
                            && Strings.isNotBlank(dashboardId)
                            && authDetailsService.isPublicDashboard(company, dashboardId)) {
                        userType = RoleType.PUBLIC_DASHBOARD.toString();
                    }

                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(new SimpleGrantedAuthority(userType)));
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // After setting the Authentication in the context, we specify
                    // that the current user is authenticated. So it passes the Spring Security Configurations successfully.
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    request.getSession().setAttribute("session_user", username);
                    request.getSession().setAttribute("company", company);
                    request.getSession().setAttribute("user_type", userType);
                    request.getSession().setAttribute("scopes", scopes);
                    Map<String, String> entitlementsConfig = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(entitlements)) {
                        entitlements.stream()
                                .filter(entitlement -> entitlement.name().contains("COUNT"))
                                .forEach(entry -> entitlementsConfig.put(entry.name().substring(0, entry.name().lastIndexOf("_")), entry.name().substring(entry.name().lastIndexOf("_") + 1)));
                    }
                    request.getSession().setAttribute("entitlementsConfig", entitlementsConfig);
                    request.getSession().setAttribute("accessContext", AccessContext.builder().build());
                    request.getSession().setAttribute("defaultEntitlements", Set.of());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean checkAccess(List<Entitlements> entitlements, String uri, String method) {

        Map<Entitlements, Set<EntitlementDetails>> entitlementsMap = EntitlementAPIMappingConfig.entitlementsMap;
        Map<Entitlements, Set<EntitlementDetails>> readOnlyEntitlementsMap = EntitlementAPIMappingConfig.readOnlyEntitlementsMap;
        boolean allowAccess = false;
        for (Entitlements entitlement : entitlements) {
            if (entitlement.getCategory() == EntitlementsCategory.WRITE_CATEGORY && entitlementsMap.containsKey(entitlement)) {
                allowAccess = entitlementsMap.get(entitlement).stream()
                        .anyMatch(detail -> uri.startsWith(detail.getApi()));
            }else if (readOnlyEntitlementsMap.containsKey(entitlement)) {
                allowAccess = readOnlyEntitlementsMap.get(entitlement).stream()
                        .anyMatch(detail -> Pattern.compile(detail.getApi()).matcher(uri).matches() && method.equals(detail.getMethod()));
            }
            if(allowAccess)
                break;
        }

        return allowAccess;
    }


    @Nonnull
    private String[] getTokens(String requestTokenHeader, String apiKey, String authParam) {
        if (requestTokenHeader != null) {
            return requestTokenHeader.split(" ");
        } else if (apiKey != null) {
            return new String[]{APIKEY, apiKey};
        } else if (authParam != null) {
            return new String[]{AUTHORIZATION, authParam};
        } else {
            throw new IllegalArgumentException("requestTokenHeader and query param apiKey both are null");
        }
    }
}
