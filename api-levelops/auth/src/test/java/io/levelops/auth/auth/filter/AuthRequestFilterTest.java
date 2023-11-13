package io.levelops.auth.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.auth.utils.TenantUtilService;
import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.models.ApiKeyToken;
import io.levelops.commons.models.DbListResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthRequestFilterTest {


    private AuthDetailsService authDetailsService;
    private ObjectMapper objectMapper;
    private  JwtTokenUtil jwtTokenUtil;
    private  RedisConnectionFactory redisConnectionFactory;
    private AuthRequestFilter authRequestFilter;

    private AccessKeyService keyService;
    private UserService userService;
    @Mock
    private TenantService tenantService;
    @InjectMocks
    private TenantUtilService tenantUtilService;

    @Before
    public void setup(){
        redisConnectionFactory=Mockito.mock(RedisConnectionFactory.class);
        jwtTokenUtil = Mockito.mock(JwtTokenUtil.class);
        objectMapper = Mockito.mock(ObjectMapper.class);
        keyService= Mockito.mock(AccessKeyService.class);
        userService=Mockito.mock(UserService.class);
        authDetailsService = Mockito.spy(new AuthDetailsService(userService,
                keyService,Mockito.mock(PasswordEncoder.class),
                Mockito.mock(TenantConfigService.class),
                10000L,Mockito.mock(DashboardWidgetService.class),
                Mockito.mock(SecretsManagerServiceClient.class),
                redisConnectionFactory,
                tenantService,
                Mockito.mock(LicensingService.class),
                Mockito.mock(Set.class),
                tenantUtilService
        ));

        authRequestFilter = new AuthRequestFilter(jwtTokenUtil, authDetailsService, objectMapper, redisConnectionFactory, false, new Auth(true));
    }

    public static String generateChars() {
        StringBuilder string = new StringBuilder();
        for (int i = 1; i < 500; i++) {
            string.append((char) i);
        }
        return string.toString();
    }

    @Test
    public void base64() {
        String input = generateChars();
        System.out.println("Input (all chars from 0-500): " + "\n---------------\n" + input + "\n---------------\n");

        // ---- ENCODE
        String postgresBase64 = PostgresBase64.encodeBytes(input.getBytes());
        String javaBase64 = Base64.getEncoder().encodeToString(input.getBytes());

        System.out.println("base64 postgres encoded:" + "\n---------------\n" + postgresBase64 + "\n---------------\n");
        System.out.println("base64 java encoded:" + "\n---------------\n" + javaBase64 + "\n---------------\n");

        // ---- DECODE POSTGRES STRING

        System.out.println("DECODING\n---------------");

        System.out.println("Postgres decode:");
        String postgresDecoded = new String(PostgresBase64.decode(postgresBase64));
        System.out.println("match=" + postgresDecoded.equals(input));

        System.out.println("\nJava decode:");
        String javaDecoded = null;
        try {
            javaDecoded = new String(Base64.getDecoder().decode(postgresBase64.getBytes(StandardCharsets.UTF_8)));
            System.out.println("match=" + javaDecoded.equals(input));
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("\nJava MIME decode:");
        String javaMimeDecoded = new String(Base64.getMimeDecoder().decode(postgresBase64.getBytes(StandardCharsets.UTF_8)));
        System.out.println("match=" + javaMimeDecoded.equals(input));
        System.out.println("---------------");

        // ---- DECODING JAVA STRING

        System.out.println("Postgres decode:");
        String postgresDecoded2 = new String(PostgresBase64.decode(javaBase64));
        System.out.println("match=" + postgresDecoded2.equals(input));

        System.out.println("\nJava decode:");
        String javaDecoded2 = null;
        try {
            javaDecoded2 = new String(Base64.getDecoder().decode(javaBase64.getBytes(StandardCharsets.UTF_8)));
            System.out.println("match=" + javaDecoded2.equals(input));
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("\nJava MIME decode:");
        String javaMimeDecoded2 = new String(Base64.getMimeDecoder().decode(javaBase64.getBytes(StandardCharsets.UTF_8)));
        System.out.println("match=" + javaMimeDecoded2.equals(input));
        System.out.println("---------------");


        assert(input.equals(postgresDecoded2));
        assert(input.equals(javaDecoded2));
        assert(input.equals(javaMimeDecoded2));

    }


    @Test
    public void doFilterInternalTest_WhenSuccessful() throws ServletException, IOException, SQLException, ExecutionException {
        HttpServletRequest request= Mockito.mock(HttpServletRequest.class);
        MockHttpSession mockHttpSession= new MockHttpSession();
        Mockito.when(request.getHeader(Mockito.eq("Authorization"))).thenReturn("Apikey eyJrZXkiOiJva3BKWlFWJFQ5d2Rpbm9TJEFlV29GZSRmQXBqK0xvVk15NmxsQkRianZ1MSZjdkcmSyIsImlkIjoiNTlhMDVkYjctZjhlYS00MGI5LTlmZmEtMDc0ZTQ1ZmUzOTBmIiwiY29tcGFueSI6ImZvbyJ9");
        HttpServletResponse response= Mockito.mock(HttpServletResponse.class);
        AccessKey key= AccessKey.builder().id("id").bcryptSecret("secret").description("desc").roleType(RoleType.ADMIN).build();
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        FilterChain chain= Mockito.mock(FilterChain.class);
        String id=UUID.randomUUID().toString();
        ApiKeyToken apiKeyToken= ApiKeyToken.builder().key("secret").id(id).company("foo").build();
        Mockito.when(keyService.getForAuthOnly(Mockito.any(),Mockito.anyString())).thenReturn(Optional.ofNullable(key));
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());;
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class),Mockito.eq(ApiKeyToken.class))).thenReturn(apiKeyToken);
        Mockito.when(authDetailsService.isMatchInvalid(Mockito.any(),Mockito.any())).thenReturn(false);
        Mockito.when(request.getSession()).thenReturn(mockHttpSession);
        authRequestFilter.doFilterInternal(request,response,chain);
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
    }

    @Test
    public void doFilterInternalTest_CacheCheck() throws ServletException, IOException, SQLException {
        HttpServletRequest request= Mockito.mock(HttpServletRequest.class);
        MockHttpSession mockHttpSession= new MockHttpSession();
        Mockito.when(request.getHeader(Mockito.eq("Authorization"))).thenReturn("Apikey eyJrZXkiOiJva3BKWlFWJFQ5d2Rpbm9TJEFlV29GZSRmQXBqK0xvVk15NmxsQkRianZ1MSZjdkcmSyIsImlkIjoiNTlhMDVkYjctZjhlYS00MGI5LTlmZmEtMDc0ZTQ1ZmUzOTBmIiwiY29tcGFueSI6ImZvbyJ9");
        HttpServletResponse response= Mockito.mock(HttpServletResponse.class);
        AccessKey key= AccessKey.builder().id("id").bcryptSecret("secret").description("desc").roleType(RoleType.ADMIN).build();
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        FilterChain chain= Mockito.mock(FilterChain.class);
        String id=UUID.randomUUID().toString();
        ApiKeyToken apiKeyToken= ApiKeyToken.builder().key("secret").id(id).company("foo").build();
        Mockito.when(keyService.getForAuthOnly(Mockito.any(),Mockito.anyString())).thenReturn(Optional.ofNullable(key));
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class),Mockito.eq(ApiKeyToken.class))).thenReturn(apiKeyToken);
        Mockito.when(authDetailsService.isMatchInvalid(Mockito.any(),Mockito.any())).thenReturn(false);
        Mockito.when(request.getSession()).thenReturn(mockHttpSession);
        authRequestFilter.doFilterInternal(request,response,chain);
        authRequestFilter.doFilterInternal(request,response,chain);
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
        Assert.assertEquals("foo",mockHttpSession.getAttribute("company"));
    }

    @Test
    public void doFilterInternalTest_WhenTenantNotExist() throws ServletException, IOException, SQLException {
        HttpServletRequest request= Mockito.mock(HttpServletRequest.class);
        MockHttpSession mockHttpSession= new MockHttpSession();
        Mockito.when(request.getHeader(Mockito.eq("Authorization"))).thenReturn("Apikey eyJrZXkiOiJva3BKWlFWJFQ5d2Rpbm9TJEFlV29GZSRmQXBqK0xvVk15NmxsQkRianZ1MSZjdkcmSyIsImlkIjoiNTlhMDVkYjctZjhlYS00MGI5LTlmZmEtMDc0ZTQ1ZmUzOTBmIiwiY29tcGFueSI6ImZvbyJ9");
        HttpServletResponse response= Mockito.mock(HttpServletResponse.class);
        AccessKey key= AccessKey.builder().id("id").bcryptSecret("secret").description("desc").roleType(RoleType.ADMIN).build();
        Tenant tenant=new Tenant("id","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        FilterChain chain= Mockito.mock(FilterChain.class);
        String id=UUID.randomUUID().toString();
        ApiKeyToken apiKeyToken= ApiKeyToken.builder().key("secret").id(id).company("foo").build();
        Mockito.when(keyService.getForAuthOnly(Mockito.any(),Mockito.anyString())).thenReturn(Optional.ofNullable(key));
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());;
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class),Mockito.eq(ApiKeyToken.class))).thenReturn(apiKeyToken);
        Mockito.when(authDetailsService.isMatchInvalid(Mockito.any(),Mockito.any())).thenReturn(false);
        Mockito.when(request.getSession()).thenReturn(mockHttpSession);
        authRequestFilter.doFilterInternal(request,response,chain);
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
        Assert.assertNotEquals("foo",mockHttpSession.getAttribute("company"));
    }

    @Test
    public void doFilterInternalTest_WhenCacheRefresh() throws ServletException, IOException, SQLException {
        HttpServletRequest request= Mockito.mock(HttpServletRequest.class);
        MockHttpSession mockHttpSession= new MockHttpSession();
        Mockito.when(request.getHeader(Mockito.eq("Authorization"))).thenReturn("Apikey eyJrZXkiOiJva3BKWlFWJFQ5d2Rpbm9TJEFlV29GZSRmQXBqK0xvVk15NmxsQkRianZ1MSZjdkcmSyIsImlkIjoiNTlhMDVkYjctZjhlYS00MGI5LTlmZmEtMDc0ZTQ1ZmUzOTBmIiwiY29tcGFueSI6ImZvbyJ9");
        HttpServletResponse response= Mockito.mock(HttpServletResponse.class);
        AccessKey key= AccessKey.builder().id("id").bcryptSecret("secret").description("desc").roleType(RoleType.ADMIN).build();
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        FilterChain chain= Mockito.mock(FilterChain.class);
        String id=UUID.randomUUID().toString();
        ApiKeyToken apiKeyToken= ApiKeyToken.builder().key("secret").id(id).company("foo").build();
        Mockito.when(keyService.getForAuthOnly(Mockito.any(),Mockito.anyString())).thenThrow(SQLException.class);
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());;
        Mockito.when(objectMapper.readValue(Mockito.any(byte[].class),Mockito.eq(ApiKeyToken.class))).thenReturn(apiKeyToken);
        Mockito.when(authDetailsService.isMatchInvalid(Mockito.any(),Mockito.any())).thenReturn(false);
        Mockito.when(request.getSession()).thenReturn(mockHttpSession);
        authRequestFilter.doFilterInternal(request,response,chain);
        Mockito.verify(tenantService,Mockito.times(3)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
        Assert.assertNotEquals("foo",mockHttpSession.getAttribute("company"));
    }




}