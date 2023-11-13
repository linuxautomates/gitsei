package io.levelops.auth.auth.filter;

import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.client.ACLClientFactory;
import io.harness.authz.acl.model.AccessCheckDataResponse;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.AccessControlDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.HttpTestUtils;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.auth.utils.ControllerMethodFinder;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class HarnessAuthRequestFilterTest {

    private ACLClientFactory aclClientFactory;
    private ControllerMethodFinder controllerMethodFinder;
    private ObjectMapper objectMapper;
    private JwtTokenUtil jwtTokenUtil;
    private String identityServiceSecret = "";
    private HarnessAuthRequestFilter harnessAuthRequestFilter;

    private AuthDetailsService authDetailsService;

    @Before
    public void setup(){
        aclClientFactory = Mockito.mock(ACLClientFactory.class);
        controllerMethodFinder = Mockito.mock(ControllerMethodFinder.class);
        jwtTokenUtil = Mockito.mock(JwtTokenUtil.class);
        authDetailsService = Mockito.mock(AuthDetailsService.class);
        objectMapper = DefaultObjectMapper.get();

        harnessAuthRequestFilter = new HarnessAuthRequestFilter(aclClientFactory, controllerMethodFinder, objectMapper, jwtTokenUtil, identityServiceSecret, new Auth(false), Set.of(), authDetailsService);
    }

    @Test
    public void Test() throws ServletException, IOException, NoSuchMethodException, ACLClientException {

        FilterChain filterChain = Mockito.mock(FilterChain.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        ACLClient aclClient = Mockito.mock(ACLClient.class);

        String token = "IdentityService eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.";
        String requestURI = "http://app.harness.io/sei/api/v1/org/workspaces/list";
        HttpServletRequest req = HttpTestUtils.getHttpRequest(requestURI,token);

        Claim companyClaim = HttpTestUtils.getClaim("kmpySmUISimoRrJL6NL73w");
        Claim principal = HttpTestUtils.getClaim("lv0euRhKRCyiXWzS7pOg6g");
        Claim principalType = HttpTestUtils.getClaim("USER");
        Claim email = HttpTestUtils.getClaim("user@harness.io");

        when(jwtTokenUtil.verifyJWTToken(any(), any())).thenReturn(Map.of("accountId", companyClaim,
                "email", principal, "type", principalType, "name", email));

        Class<?> myClass = HarnessAuthRequestFilterTest.class;
        Method method = myClass.getMethod("dummyMethod");
        when(controllerMethodFinder.findControllerMethod(any(), any())).thenReturn(method);

       when(aclClientFactory.get(any())).thenReturn(aclClient);
       when(aclClient.checkAccess(any())).thenReturn(AccessCheckResponseDTO.builder()
                       .accessCheckDataResponse(AccessCheckDataResponse.builder()
                               .accessControlList(List.of(AccessControlDTO.builder().permitted(false).build()))
                               .build())
               .build());

        harnessAuthRequestFilter.doFilterInternal(req, res, filterChain);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    public void dummyMethod(){
    }


    @Test
    public void apiKeyTest() throws SQLException, ExecutionException, IllegalAccessException {
        String token = "Apikey eyJrZXkiOiI1XjJSb3FDME95OTdoOUpzaGdnRExadV5ZVXhraUxAMlduQlJDWFlecGpHUl9yYTRKTCIsImlkIjoiY2ZlZGU5MDQtMzZjOS00ZWY1LWExZTYtNmYyYzlkNTk5N2QyIiwiY29tcGFueSI6ImF1dG9tYXRpb24ifQ==";
        String requestURI = "http://app.harness.io/sei/api/v1/org/workspaces/list";

        FilterChain filterChain = Mockito.mock(FilterChain.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        when(authDetailsService.validateKeyAndGetRole(any(),any(),any())).thenThrow(new UsernameNotFoundException("User not found!"));
        HttpServletRequest req = HttpTestUtils.getHttpRequest(requestURI,token);
        try {
            harnessAuthRequestFilter.doFilterInternal(req, res, filterChain);
        } catch (Exception e) {
            Assertions.assertThat(e.getMessage()).isEqualTo("Failed to process request, exception occurred - User not found!");
        }
    }
}
