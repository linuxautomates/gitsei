package io.levelops.auth.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.WebSecurityConfig;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.config.DefaultTestConfiguration;
import io.levelops.auth.httpmodels.JwtResponse;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.notification.services.TenantManagementNotificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = JwtAuthenticationController.class)
@ContextConfiguration(classes = {JwtAuthenticationController.class, WebSecurityConfig.class, DefaultTestConfiguration.class})
@SuppressWarnings("unused")
public class JwtAuthenticationControllerTest {
    @Autowired
    private AuthDetailsService authDetailsService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private SamlConfigService samlConfigService;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private TenantManagementNotificationService tenantManagementNotificationService;

    private String testIdpPrivateKey = "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANnlTDUJWNBnlm8y\n" +
            "eIh+QrwLlZwDvY/DYtHlnibto0G3y1zB04OI+I/w4CgtsuLcoraJjwGv5HE9mZAu\n" +
            "Dc6xfGEe8ZdWlS0Eb06fPFyFWeD44cbLmTgBCqT9tmPktiTd/QEHP1iUw0aSCr9w\n" +
            "QcKruXj6Osv2WCraLMVyrEFtfyC5AgMBAAECgYEAg3cCLLmv+UWWkQ6dhJTRRM2k\n" +
            "fZYQKA4VsFhevFQCfSMcMqHLMQBSH96QEA3KcW/3SDTksHrkVKJ65W+z6vJQ/NbW\n" +
            "mxlsxRjTEZRamgwPDY7QsWOx34X9lHgH1ECShbsG1WkagWrUqM6ZeOQPvfYxsETc\n" +
            "RRT0T/0tTB0vlRsYwIUCQQD0q8QPXVb+fnyxnj51oxKgnFShm0r4hUQzrrr0A7j+\n" +
            "axYgqj4bX2r0FnqUr3pG2EieKmCEzXofQtHnb6ESPEIPAkEA4/wl9GgCAx3pLl2p\n" +
            "yrDlwbdpXpMMAC5zhZaXTuFEdEs1gR/UyGA9uGS0cE0eJlPJkFCN7j2U9xcvcohr\n" +
            "Rp2YtwJBAMdJ5T9ykrpmQqDWOR/OfQyvaUvc1rsIqZ3DM+ov4g3xIavnnwDivpYk\n" +
            "fSIAsINmBfg2ojlpuJAf+CMfzL2ysUcCQCagsk2W0cB1cOzKMdqF3mfUC6Zag84E\n" +
            "EM6xlTFoOZD6rTYTjpnktJBpf6kHZ/RWffBVtbaH+JVk+EUPmB1+1kECQCogqzyI\n" +
            "jkjb5FfGfvyMQfMkY8PxehQoAifGcjXFDXizbC7+El5J89mv7YDoUIP3no1FpP9C\n" +
            "io/fLXC8uvNgxsA=\n" +
            "-----END PRIVATE KEY-----";
    private String testPublicKey = "-----BEGIN CERTIFICATE-----\n" +
            "MIICNDCCAZ2gAwIBAgIBADANBgkqhkiG9w0BAQ0FADA3MQswCQYDVQQGEwJ1czEM\n" +
            "MAoGA1UECAwDYXNkMQwwCgYDVQQKDANhc2QxDDAKBgNVBAMMA2FzZDAeFw0xOTA5\n" +
            "MTcxOTI4MzhaFw0yOTA5MTQxOTI4MzhaMDcxCzAJBgNVBAYTAnVzMQwwCgYDVQQI\n" +
            "DANhc2QxDDAKBgNVBAoMA2FzZDEMMAoGA1UEAwwDYXNkMIGfMA0GCSqGSIb3DQEB\n" +
            "AQUAA4GNADCBiQKBgQDZ5Uw1CVjQZ5ZvMniIfkK8C5WcA72Pw2LR5Z4m7aNBt8tc\n" +
            "wdODiPiP8OAoLbLi3KK2iY8Br+RxPZmQLg3OsXxhHvGXVpUtBG9OnzxchVng+OHG\n" +
            "y5k4AQqk/bZj5LYk3f0BBz9YlMNGkgq/cEHCq7l4+jrL9lgq2izFcqxBbX8guQID\n" +
            "AQABo1AwTjAdBgNVHQ4EFgQUYHB845HJf/ZdJ4lrh5sB5sOXqWowHwYDVR0jBBgw\n" +
            "FoAUYHB845HJf/ZdJ4lrh5sB5sOXqWowDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B\n" +
            "AQ0FAAOBgQCxTnCtvJSJ7ErBnUxk9fOcY7sUSzKUCxLw4fQ5V0X+Kgl2OGgfHfbo\n" +
            "HTmU74HqvhGar6uhUhDXBdYvw5ABxGvICZ4sOhhxesUziNIeDNV1JPQLRKGGIrV1\n" +
            "RocTzxYHt6rrpBkKzgBOSmUJ5MpyzDsav+lI1/q9iIe84EFN3EoeFA==\n" +
            "-----END CERTIFICATE-----";

    private String testSamlResponse = "PD94bWwgdmVyc2lvbj0iMS4wIj8+CjxzYW1scDpSZXNwb25zZSB4bWxuczpzYW1scD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnByb3RvY29sIiB4bWxuczpzYW1sPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIiBJRD0icGZ4M2NmMWYxZTEtMTUzOC05MDhhLTQ3NzktMjg4N2U4YzRmZmFjIiBWZXJzaW9uPSIyLjAiIElzc3VlSW5zdGFudD0iMjAxNC0wNy0xN1QwMTowMTo0OFoiIERlc3RpbmF0aW9uPSJodHRwczovL2FwaS5sZXZlbG9wcy5pby92MS9zYW1sX2F1dGgiPgogIDxzYW1sOklzc3Vlcj50ZXN0MTwvc2FtbDpJc3N1ZXI+PGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgPGRzOlNpZ25lZEluZm8+PGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz4KICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz4KICA8ZHM6UmVmZXJlbmNlIFVSST0iI3BmeDNjZjFmMWUxLTE1MzgtOTA4YS00Nzc5LTI4ODdlOGM0ZmZhYyI+PGRzOlRyYW5zZm9ybXM+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHM6VHJhbnNmb3Jtcz48ZHM6RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTEiLz48ZHM6RGlnZXN0VmFsdWU+VUYxdkplbS9RRUJFbFFQWmJRZkd0Z1VmTFU4PTwvZHM6RGlnZXN0VmFsdWU+PC9kczpSZWZlcmVuY2U+PC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT4xclVybk9HSW00ZjZ6cmtQYUpTeFoxQ1doaVlteHIzMWhBNEdYbk92MXNRRkR2SGhEdnNGZEZyR2hDK2Z5L08vczh0bGNIdFpZUDB3dkZqZTNqYnN5WlhIT1F0R20yVzdDU3RwQnZnbU1aa05PbTdudFNhY0FHR29BR1k5QWduVmdFaXBOaGg5THArRzlHejI5UWtJM2RKWUEzRkVZaXFFWmJwcUY1cVBSd1E9PC9kczpTaWduYXR1cmVWYWx1ZT4KPGRzOktleUluZm8+PGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU+TUlJQ05EQ0NBWjJnQXdJQkFnSUJBREFOQmdrcWhraUc5dzBCQVEwRkFEQTNNUXN3Q1FZRFZRUUdFd0oxY3pFTU1Bb0dBMVVFQ0F3RFlYTmtNUXd3Q2dZRFZRUUtEQU5oYzJReEREQUtCZ05WQkFNTUEyRnpaREFlRncweE9UQTVNVGN4T1RJNE16aGFGdzB5T1RBNU1UUXhPVEk0TXpoYU1EY3hDekFKQmdOVkJBWVRBblZ6TVF3d0NnWURWUVFJREFOaGMyUXhEREFLQmdOVkJBb01BMkZ6WkRFTU1Bb0dBMVVFQXd3RFlYTmtNSUdmTUEwR0NTcUdTSWIzRFFFQkFRVUFBNEdOQURDQmlRS0JnUURaNVV3MUNWalFaNVp2TW5pSWZrSzhDNVdjQTcyUHcyTFI1WjRtN2FOQnQ4dGN3ZE9EaVBpUDhPQW9MYkxpM0tLMmlZOEJyK1J4UFptUUxnM09zWHhoSHZHWFZwVXRCRzlPbnp4Y2hWbmcrT0hHeTVrNEFRcWsvYlpqNUxZazNmMEJCejlZbE1OR2tncS9jRUhDcTdsNCtqckw5bGdxMml6RmNxeEJiWDhndVFJREFRQUJvMUF3VGpBZEJnTlZIUTRFRmdRVVlIQjg0NUhKZi9aZEo0bHJoNXNCNXNPWHFXb3dId1lEVlIwakJCZ3dGb0FVWUhCODQ1SEpmL1pkSjRscmg1c0I1c09YcVdvd0RBWURWUjBUQkFVd0F3RUIvekFOQmdrcWhraUc5dzBCQVEwRkFBT0JnUUN4VG5DdHZKU0o3RXJCblV4azlmT2NZN3NVU3pLVUN4THc0ZlE1VjBYK0tnbDJPR2dmSGZib0hUbVU3NEhxdmhHYXI2dWhVaERYQmRZdnc1QUJ4R3ZJQ1o0c09oaHhlc1V6aU5JZUROVjFKUFFMUktHR0lyVjFSb2NUenhZSHQ2cnJwQmtLemdCT1NtVUo1TXB5ekRzYXYrbEkxL3E5aUllODRFRk4zRW9lRkE9PTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE+PC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPgogIDxzYW1scDpTdGF0dXM+CiAgICA8c2FtbHA6U3RhdHVzQ29kZSBWYWx1ZT0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnN0YXR1czpTdWNjZXNzIi8+CiAgPC9zYW1scDpTdGF0dXM+CiAgPHNhbWw6QXNzZXJ0aW9uIHhtbG5zOnhzaT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEtaW5zdGFuY2UiIHhtbG5zOnhzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSIgSUQ9InBmeGY3ZTMyMmY2LTNlYmUtMDg4Zi0zODRkLTE4ZWI4NmExOWQwMSIgVmVyc2lvbj0iMi4wIiBJc3N1ZUluc3RhbnQ9IjIwMTQtMDctMTdUMDE6MDE6NDhaIj4KICAgIDxzYW1sOklzc3Vlcj50ZXN0MTwvc2FtbDpJc3N1ZXI+PGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnIyI+CiAgPGRzOlNpZ25lZEluZm8+PGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz4KICAgIDxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz4KICA8ZHM6UmVmZXJlbmNlIFVSST0iI3BmeGY3ZTMyMmY2LTNlYmUtMDg4Zi0zODRkLTE4ZWI4NmExOWQwMSI+PGRzOlRyYW5zZm9ybXM+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHM6VHJhbnNmb3Jtcz48ZHM6RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTEiLz48ZHM6RGlnZXN0VmFsdWU+UEt6Q0ppSlR0RnJzMG44QlpCZDkrbTFZQ2drPTwvZHM6RGlnZXN0VmFsdWU+PC9kczpSZWZlcmVuY2U+PC9kczpTaWduZWRJbmZvPjxkczpTaWduYXR1cmVWYWx1ZT5WWHhJdnJlU0xmcU1PZG1iYzV6Z3lTbjJGek9UUWZlK1lMaHgzZTNaeWZtampPVEVaTnZCZmxEU2NheWZMQWFUclJwSzgrZ3RuaFZsVXpMTXI0NFZJNzR1OUp2QlE3c0s4bERac0ZIdHBvTm03aWhmNDZBTWw3RG1qd2M1aC9rYlZtUnhZQjltd09Ea1RnUVlXT0tqRDNvYmxTY0hidlBSKyt3K3pWTUhJZnc9PC9kczpTaWduYXR1cmVWYWx1ZT4KPGRzOktleUluZm8+PGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU+TUlJQ05EQ0NBWjJnQXdJQkFnSUJBREFOQmdrcWhraUc5dzBCQVEwRkFEQTNNUXN3Q1FZRFZRUUdFd0oxY3pFTU1Bb0dBMVVFQ0F3RFlYTmtNUXd3Q2dZRFZRUUtEQU5oYzJReEREQUtCZ05WQkFNTUEyRnpaREFlRncweE9UQTVNVGN4T1RJNE16aGFGdzB5T1RBNU1UUXhPVEk0TXpoYU1EY3hDekFKQmdOVkJBWVRBblZ6TVF3d0NnWURWUVFJREFOaGMyUXhEREFLQmdOVkJBb01BMkZ6WkRFTU1Bb0dBMVVFQXd3RFlYTmtNSUdmTUEwR0NTcUdTSWIzRFFFQkFRVUFBNEdOQURDQmlRS0JnUURaNVV3MUNWalFaNVp2TW5pSWZrSzhDNVdjQTcyUHcyTFI1WjRtN2FOQnQ4dGN3ZE9EaVBpUDhPQW9MYkxpM0tLMmlZOEJyK1J4UFptUUxnM09zWHhoSHZHWFZwVXRCRzlPbnp4Y2hWbmcrT0hHeTVrNEFRcWsvYlpqNUxZazNmMEJCejlZbE1OR2tncS9jRUhDcTdsNCtqckw5bGdxMml6RmNxeEJiWDhndVFJREFRQUJvMUF3VGpBZEJnTlZIUTRFRmdRVVlIQjg0NUhKZi9aZEo0bHJoNXNCNXNPWHFXb3dId1lEVlIwakJCZ3dGb0FVWUhCODQ1SEpmL1pkSjRscmg1c0I1c09YcVdvd0RBWURWUjBUQkFVd0F3RUIvekFOQmdrcWhraUc5dzBCQVEwRkFBT0JnUUN4VG5DdHZKU0o3RXJCblV4azlmT2NZN3NVU3pLVUN4THc0ZlE1VjBYK0tnbDJPR2dmSGZib0hUbVU3NEhxdmhHYXI2dWhVaERYQmRZdnc1QUJ4R3ZJQ1o0c09oaHhlc1V6aU5JZUROVjFKUFFMUktHR0lyVjFSb2NUenhZSHQ2cnJwQmtLemdCT1NtVUo1TXB5ekRzYXYrbEkxL3E5aUllODRFRk4zRW9lRkE9PTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE+PC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPgogICAgPHNhbWw6U3ViamVjdD4KICAgICAgPHNhbWw6TmFtZUlEIFNQTmFtZVF1YWxpZmllcj0ibGV2ZWxvcHMuaW8iIEZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6ZW1haWxBZGRyZXNzIj5hc2RAYXNkLmNvbTwvc2FtbDpOYW1lSUQ+CiAgICAgIDxzYW1sOlN1YmplY3RDb25maXJtYXRpb24gTWV0aG9kPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6Y206YmVhcmVyIj4KICAgICAgICA8c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uRGF0YSBOb3RPbk9yQWZ0ZXI9IjIwMjQtMDEtMThUMDY6MjE6NDhaIiBSZWNpcGllbnQ9Imh0dHBzOi8vYXBpLmxldmVsb3BzLmlvL3YxL3NhbWxfYXV0aCIvPgogICAgICA8L3NhbWw6U3ViamVjdENvbmZpcm1hdGlvbj4KICAgIDwvc2FtbDpTdWJqZWN0PgogICAgPHNhbWw6Q29uZGl0aW9ucyBOb3RCZWZvcmU9IjIwMTQtMDctMTdUMDE6MDE6MThaIiBOb3RPbk9yQWZ0ZXI9IjIwMjQtMDEtMThUMDY6MjE6NDhaIj4KICAgICAgPHNhbWw6QXVkaWVuY2VSZXN0cmljdGlvbj4KICAgICAgICA8c2FtbDpBdWRpZW5jZT5sZXZlbG9wcy5pbzwvc2FtbDpBdWRpZW5jZT4KICAgICAgPC9zYW1sOkF1ZGllbmNlUmVzdHJpY3Rpb24+CiAgICA8L3NhbWw6Q29uZGl0aW9ucz4KICAgIDxzYW1sOkF1dGhuU3RhdGVtZW50IEF1dGhuSW5zdGFudD0iMjAxNC0wNy0xN1QwMTowMTo0OFoiIFNlc3Npb25Ob3RPbk9yQWZ0ZXI9IjIwMjQtMDctMTdUMDk6MDE6NDhaIiBTZXNzaW9uSW5kZXg9Il9iZTk5NjdhYmQ5MDRkZGNhZTNjMGViNDE4OWFkYmUzZjcxZTMyN2NmOTMiPgogICAgICA8c2FtbDpBdXRobkNvbnRleHQ+CiAgICAgICAgPHNhbWw6QXV0aG5Db250ZXh0Q2xhc3NSZWY+dXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFjOmNsYXNzZXM6UGFzc3dvcmQ8L3NhbWw6QXV0aG5Db250ZXh0Q2xhc3NSZWY+CiAgICAgIDwvc2FtbDpBdXRobkNvbnRleHQ+CiAgICA8L3NhbWw6QXV0aG5TdGF0ZW1lbnQ+CiAgICA8c2FtbDpBdHRyaWJ1dGVTdGF0ZW1lbnQ+CiAgICAgIDxzYW1sOkF0dHJpYnV0ZSBOYW1lPSJGaXJzdE5hbWUiIE5hbWVGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphdHRybmFtZS1mb3JtYXQ6YmFzaWMiPgogICAgICAgIDxzYW1sOkF0dHJpYnV0ZVZhbHVlIHhzaTp0eXBlPSJ4czpzdHJpbmciPnRlc3RAZXhhbXBsZS5jb208L3NhbWw6QXR0cmlidXRlVmFsdWU+CiAgICAgIDwvc2FtbDpBdHRyaWJ1dGU+CiAgICAgIDxzYW1sOkF0dHJpYnV0ZSBOYW1lPSJMYXN0TmFtZSIgTmFtZUZvcm1hdD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmF0dHJuYW1lLWZvcm1hdDpiYXNpYyI+CiAgICAgICAgPHNhbWw6QXR0cmlidXRlVmFsdWUgeHNpOnR5cGU9InhzOnN0cmluZyI+dXNlcnM8L3NhbWw6QXR0cmlidXRlVmFsdWU+CiAgICAgIDwvc2FtbDpBdHRyaWJ1dGU+CiAgICA8L3NhbWw6QXR0cmlidXRlU3RhdGVtZW50PgogIDwvc2FtbDpBc3NlcnRpb24+Cjwvc2FtbHA6UmVzcG9uc2U+";

    @Test
    public void testRefreshToken() throws Exception {
        reset(activityLogService);
        when(authDetailsService.getUserFromDb(anyString(), anyString())).thenReturn(User.builder().id("asd")
                .email("bsd").build());
        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
        MvcResult result = mvc.perform(post("/v1/authenticate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"harsh\"," +
                        "\"password\":\"testestestest\"," +
                        "\"company\":\"test\"}").characterEncoding("UTF-8"))
                .andExpect(status().isOk()).andReturn();
        JwtResponse map = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);

        mvc.perform(post("/v1/refresh").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + map.getToken()))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/refresh").contentType(MediaType.APPLICATION_JSON)
                .param("Authorization", map.getToken()))
                .andExpect(status().isOk());
        Thread.sleep(1000); //for tests we have defined token expiry as 1 second
        mvc.perform(post("/v1/refresh").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + map.getToken()))
                .andExpect(status().isUnauthorized());
        verify(activityLogService, times(3))
                .insert(any(), any());
    }

    @Test
    public void testIfSsoWorks() throws Exception {
        String idpCert = new String(Base64.getEncoder().encode((testPublicKey).getBytes()));
        when(samlConfigService.get(eq("test"), eq("1"))).thenReturn(
                Optional.of(SamlConfig.builder().id("1").idpSsoUrl("https://asd.com/idp_sso_url").idpCert(idpCert)
                        .enabled(Boolean.TRUE).idpId("test1").build()));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/generate_authn")
                .param("company", "test"))
                .andReturn()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("https://asd.com/idp_sso_url?SAMLRequest=*&RelayState=dGVzdA%3D%3D"));
        when(authDetailsService.loadOrProvisionUser(eq(User.builder().email("asd@asd.com")
                .firstName("test@example.com").lastName("users").build()), eq("test"))).thenReturn(
                new ExtendedUser("asd@asd.com", "asd", false, true,
                        RoleType.ADMIN, "asdom", "asd", Collections.emptyList(), false, false, false, null));
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/saml_auth")
                .param("SAMLResponse", testSamlResponse)
                .param("RelayState", "dGVzdA==")
                .header("X-Forwarded-Host", "api.levelops.io")
                .header("X-Forwarded-Port", 443)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Ssl", true))
                .andReturn()))
                .andExpect(status().isFound());
        when(authDetailsService.loadOrProvisionUser(eq(User.builder().email("asd@asd.com")
                .firstName("test@example.com").lastName("users").build()), eq("test"))).thenReturn(
                new ExtendedUser("asd@asd.com", "asd", false, false,
                        RoleType.ADMIN, "asdom", "asd", Collections.emptyList(), false, false, false, null));
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/saml_auth")
                .param("SAMLResponse", testSamlResponse)
                .param("RelayState", "dGVzdA==")
                .header("X-Forwarded-Host", "api.levelops.io")
                .header("X-Forwarded-Port", 443)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Ssl", true))
                .andReturn()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("https://app.levelops.io/auth-callback?error=sso_disabled_for_user"));
    }

    @Test
    public void testIfCorsWorksAsIntended() throws Exception {
        mvc.perform(post("/v1/saml_auth").header("origin", "https://dev-525268.okta.com"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/refresh").header("origin", "https://dev-525268.okta.com"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/generate_authn").header("origin", "https://dev-525268.okta.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testValidLogin() throws Exception {
        reset(activityLogService);
        when(authDetailsService.getUserFromDb(anyString(), anyString())).thenReturn(User.builder().id("asd")
                .email("bsd").build());
        mvc.perform(post("/v1/authenticate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"harsh\"," +
                        "\"password\":\"testestestest\"," +
                        "\"company\":\"test\"}"))
                .andExpect(status().isOk());
        verify(activityLogService, times(1))
                .insert(any(), any());
    }

    @Test
    public void testInvalidLogin() throws Exception {
        reset(activityLogService);
        when(authDetailsService.getUserFromDb(anyString(), anyString())).thenReturn(User.builder().id("asd")
                .email("bsd").build());
        //fail due to passwordauthdisabled
        mvc.perform(post("/v1/authenticate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"harsh2\"," +
                        "\"password\":\"testestestest\"," +
                        "\"company\":\"test\"}"))
                .andExpect(status().isUnauthorized());
        //fail due to invalid company
        mvc.perform(post("/v1/authenticate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"harsh\"," +
                        "\"password\":\"testestest\"," +
                        "\"company\":\"test2\"}"))
                .andExpect(status().isUnauthorized());
        //fail due to invalid password
        mvc.perform(post("/v1/authenticate").contentType(MediaType.APPLICATION_JSON).content(
                "{\"username\":\"harsh\"," +
                        "\"password\":\"alskdnaklsdn\"," +
                        "\"company\":\"test\"}"))
                .andExpect(status().isUnauthorized());
        verify(activityLogService, times(3)).insert(any(), any());
    }

    @Test
    public void testSanitizeEmail() {
        assertThat(JwtAuthenticationController.sanitizeEmail("maxime@propelo.io")).isEqualTo("maxime@propelo.io");
        assertThat(JwtAuthenticationController.sanitizeEmail("maxime+test@propelo.io")).isEqualTo("maxime+test@propelo.io");
        assertThat(JwtAuthenticationController.sanitizeEmail("maxime test@propelo.io")).isEqualTo("maxime+test@propelo.io");
    }
}
