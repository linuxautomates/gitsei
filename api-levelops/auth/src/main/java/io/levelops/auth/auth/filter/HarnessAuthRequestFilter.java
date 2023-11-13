package io.levelops.auth.auth.filter;

import com.amazonaws.services.acmpca.model.InvalidRequestException;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.client.ACLClientFactory;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.PermissionCheckDTO;
import io.harness.authz.acl.model.Principal;
import io.harness.authz.acl.model.PrincipalType;
import io.harness.authz.acl.model.ResourceScope;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.auth.utils.ControllerMethodFinder;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ApiKeyToken;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static io.levelops.commons.client.ClientConstants.APIKEY;

@Log4j2
public class HarnessAuthRequestFilter extends OncePerRequestFilter {
    private final ACLClientFactory aclClientFactory;
    private final ControllerMethodFinder controllerMethodFinder;
    private final AuthDetailsService authDetailsService;

    private final ObjectMapper objectMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final String identityServiceSecret;
    private final Auth auth;

    private static final String IDENTITY_SERVICE_TOKEN_PREFIX = "IdentityService";
    private static final String PRINCIPAL_TYPE = "type";
    private static final String PRINCIPAL_NAME = "name";
    private static final String ACCOUNT_ID = "accountId";
    private static final String EMAIL = "email";
    private final Set<String> defaultEntitlements;


    public HarnessAuthRequestFilter(ACLClientFactory aclClientFactory, ControllerMethodFinder controllerMethodFinder, ObjectMapper objectMapper,
                                    JwtTokenUtil jwtTokenUtil,
                                    String identityServiceSecret, Auth auth, Set<String> defaultEntitelements, AuthDetailsService authDetailsService) {
        this.aclClientFactory = aclClientFactory;
        this.controllerMethodFinder = controllerMethodFinder;
        this.objectMapper = objectMapper;
        this.jwtTokenUtil = jwtTokenUtil;
        this.identityServiceSecret = identityServiceSecret;
        this.auth = auth;
        this.defaultEntitlements = defaultEntitelements;
        this.authDetailsService = authDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        AccessContext.AccessContextBuilder accessContextBuilder = AccessContext.builder();

        if (!auth.isLegacy()) {

            final String requestTokenHeader = request.getHeader("Authorization");

            if (requestTokenHeader != null) {

                final String requestUrl = request.getRequestURI();
                final String requestMethod = request.getMethod();

                String company = null;
                String username = null;
                String usertype = null;
                String email = null;

                try {
                    String[] tokens = requestTokenHeader.split(" ");

                    if(!isValidRequestTokenHeader(tokens[0])){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported request header");
                    }

                    String identityToken = null;

                    if (tokens.length == 2 && IDENTITY_SERVICE_TOKEN_PREFIX.equalsIgnoreCase(tokens[0])) {

                        identityToken = tokens[1];

                        Map<String, Claim> claimsMap = jwtTokenUtil.verifyJWTToken(identityToken, identityServiceSecret);

                        company = claimsMap.get(ACCOUNT_ID).asString();
                        username = claimsMap.get(PRINCIPAL_NAME).asString();
                        usertype = claimsMap.get(PRINCIPAL_TYPE).asString();
                        email = claimsMap.get(EMAIL).asString();

                        Principal principal = Principal.builder()
                                .principalType(PrincipalType.valueOf(usertype))
                                .principalIdentifier(username)
                                .build();
                        accessContextBuilder.principal(principal);

                        ResourceScope.ResourceScopeBuilder resourceScopeBuilder =  ResourceScope.builder()
                                .accountIdentifier(company);

                        String orgIdentifier = request.getParameter("orgIdentifier");
                        String projectIdentifier = request.getParameter("projectIdentifier");

                        if(orgIdentifier != null){
                            resourceScopeBuilder.orgIdentifier(orgIdentifier);
                        }

                        if(projectIdentifier != null){
                            resourceScopeBuilder.projectIdentifier(projectIdentifier);
                        }

                        ResourceScope resourceScope = resourceScopeBuilder.build();
                        accessContextBuilder.resourceScope(resourceScope);
                        accessContextBuilder.aclClient(getAclClient(identityToken));

                        Method controllerMethod = controllerMethodFinder.findControllerMethod(requestUrl, requestMethod);

                        if (controllerMethod != null && controllerMethod.isAnnotationPresent(HarnessAccessControlCheck.class)) {

                            List<PermissionCheckDTO> permissionCheckDTOList = getPermissions(company, request, controllerMethod, resourceScope);

                            AccessCheckRequestDTO accessCheckRequestDTO = AccessCheckRequestDTO.builder()
                                    .principal(principal)
                                    .permissions(permissionCheckDTOList)
                                    .build();

                            boolean isAuthorized = false;
                            try {
                                isAuthorized = makeAuthorizationAPICall(accessCheckRequestDTO, identityToken);
                            } catch (ACLClientException e) {
                                log.error("Unable to make Access control request " + e.getMessage());
                                throw new RuntimeException(e);
                            }

                            if (!isAuthorized) {
                                logger.warn(username + " user is not authorized to access " + request.getRequestURI());
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                return;
                            }
                        }
                        usertype = "ADMIN"; // to bypass legacy flow
                    }else if (APIKEY.equalsIgnoreCase(tokens[0])) {
                        try {
                            ApiKeyToken token = objectMapper.readValue(
                                    Base64.getMimeDecoder().decode(tokens[1]),
                                    ApiKeyToken.class);
                            company = token.getCompany();
                            username = token.getId();
                            RoleType roleType = authDetailsService.validateKeyAndGetRole(company, username, token.getKey());
                            usertype = roleType.toString();

                        } catch (UsernameNotFoundException | IllegalAccessException | ExecutionException e) {
                            log.error("[{} - {}] {} - {}", company, request.getRemoteAddr(), request.getRequestURL(), e.getMessage());
                            throw e;
                        } catch (BadCredentialsException | SQLException e) {
                            log.error("[{}] Failed authentication.", company, e);
                            throw e;
                        } catch (IllegalArgumentException e) {
                            log.error("[{}] Failed to decode token: method={} uri={} token='{}'", company, requestMethod, requestUrl, tokens[1]);
                            throw e;
                        }
                    }
                } catch (InvalidRequestException e) {
                    throw new InvalidRequestException("Error while verifying jwt token: Invalid JWTToken received, failed to decode the token");
                } catch (Exception e){
                    throw new RuntimeException("Failed to process request, exception occurred - "+e.getMessage());
                }

                Map<String, List<String>> scopes = Map.of();
                Map<String, String> entitlementsConfig = Map.of();
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, null, Collections.singletonList(new SimpleGrantedAuthority(usertype)));
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                request.getSession().setAttribute("company", company.toLowerCase());
                request.getSession().setAttribute("user_type", usertype);
                request.getSession().setAttribute("session_user", email);
                request.getSession().setAttribute("scopes", scopes);
                request.getSession().setAttribute("entitlementsConfig", entitlementsConfig);
                request.getSession().setAttribute("defaultEntitlements", defaultEntitlements);
                request.getSession().setAttribute("accessContext", accessContextBuilder.build());
            }else{
                throw new RuntimeException("Bad request received, missing authorization token");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidRequestTokenHeader(String token) {
        return IDENTITY_SERVICE_TOKEN_PREFIX.equalsIgnoreCase(token) || APIKEY.equalsIgnoreCase(token);
    }

    private List<PermissionCheckDTO> getPermissions(String company, HttpServletRequest request, Method controllerMethod, ResourceScope resourceScope) throws BadRequestException {

        String orgIdentifier = request.getParameter("orgIdentifier");
        String projectIdentifier = request.getParameter("projectIdentifier");
        String collectionIdentifier = request.getParameter("collectionIdentifier");
        String insightIdentifier = request.getParameter("insightIdentifier");
        String resourceIdentifier = company;

        if(orgIdentifier != null){
            resourceIdentifier = orgIdentifier;
        }

        if(projectIdentifier != null){
            resourceIdentifier = projectIdentifier;
        }

        if(collectionIdentifier != null){
            resourceIdentifier = collectionIdentifier;
            /* Comenting for now
            if(!isValidateCollectionIdentifier(request, collectionIdentifier)){
                throw new BadRequestException("Collection id in the payload and query param does not match");
            }*/
        }
        if(insightIdentifier != null){
            resourceIdentifier = insightIdentifier;
        }

        HarnessAccessControlCheck annotation = controllerMethod.getAnnotation(HarnessAccessControlCheck.class);

        ResourceType resourceType = annotation.resourceType();
        Permission permission = annotation.permission();

        if(resourceType == ResourceType.SEI_COLLECTIONS_INSIGHTS && permission == Permission.COLLECTION_INSIGHT_VIEW){

            if(collectionIdentifier == null || insightIdentifier == null){
                throw new BadRequestException("Missing collection or insight identifier in the request");
            }

            String [] permissionsArr = permission.getPermission().split(",");

            if(permissionsArr.length != 2 ){
                throw new RuntimeException("COLLECTION_INSIGHT_VIEW permission is expected to have two set of permissions collection_view and insight_view, one of these is missing");
            }

            return List.of(PermissionCheckDTO.builder()
                            .resourceScope(resourceScope)
                            .resourceType(ResourceType.SEI_COLLECTIONS.name())
                            .resourceIdentifier(collectionIdentifier)
                            .permission(permissionsArr[0])
                            .build(),
                    PermissionCheckDTO.builder()
                            .resourceScope(resourceScope)
                            .resourceType(ResourceType.SEI_INSIGHTS.name())
                            .resourceIdentifier(insightIdentifier)
                            .permission(permissionsArr[1])
                            .build());
        }else{
            return List.of(PermissionCheckDTO.builder()
                    .resourceScope(resourceScope)
                    .resourceType(resourceType.name())
                    .resourceIdentifier(resourceIdentifier)
                    .permission(permission.getPermission())
                    .build());
        }
    }

    private boolean isValidateCollectionIdentifier(HttpServletRequest request, String collectionIdentifier){

        if(RequestMethod.POST.equals(RequestMethod.valueOf(request.getMethod()))){
            try {
                BufferedReader br = request.getReader();
                DefaultListRequest req = objectMapper.readValue(br, DefaultListRequest.class);
                if(!CollectionUtils.isEmpty(req.getOuIds()) && req.getOuIds().size() == 1
                        && req.getOuIds().contains(Integer.valueOf(collectionIdentifier))){
                    return true;
                }
            }catch(IOException e){
                log.error("Error validating collectionIdentifier for request {}", request.getRequestURI());
            }
        }else if(RequestMethod.GET.equals(RequestMethod.valueOf(request.getMethod()))){
            //TODO Check for the GET pattern and validate collectionIdentifier
        }else if(RequestMethod.PUT.equals(RequestMethod.valueOf(request.getMethod()))){
            //TODO Check for the PUT pattern and validate collectionIdentifier
        }
        return false;
    }

    private boolean makeAuthorizationAPICall(AccessCheckRequestDTO accessCheckRequestDTO, String jwtToken) throws ACLClientException {
        ACLClient aclClient = getAclClient(jwtToken);
        AccessCheckResponseDTO response = aclClient.checkAccess(accessCheckRequestDTO);
        return response.getAccessCheckDataResponse().getAccessControlList().stream().allMatch(o -> o.isPermitted());
    }

    private ACLClient getAclClient(String jwtToken){
        return  aclClientFactory.get(jwtToken);
    }
}