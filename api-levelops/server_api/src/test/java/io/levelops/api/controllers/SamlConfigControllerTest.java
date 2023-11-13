package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.config.ServerConfig;
import io.levelops.api.responses.SamlConfigResponse;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SamlConfigService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ServerConfig.class, DefaultApiTestConfiguration.class})
public class SamlConfigControllerTest {
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SamlConfigService samlConfigService;
    @Autowired
    private ActivityLogService activityLogService;

    @Before
    public void setup() {
        //The non-standalone setup will require authentication and everything to be done properly.
        mvc = MockMvcBuilders.standaloneSetup(new SamlConfigController(samlConfigService,
                activityLogService, "askdj", "askjdh")).build();
    }

    @Test
    public void testCreateSamlConfig() throws Exception {
        reset(samlConfigService);
        reset(activityLogService);
        String idpCert = new String(Base64.getEncoder().encode(("-----BEGIN CERTIFICATE-----\n" +
                "MIICzjCCAbYCCQCiEaX6dJasBTANBgkqhkiG9w0BAQsFADApMQswCQYDVQQGEwJh\n" +
                "YTELMAkGA1UECAwCYWExDTALBgNVBAMMBGFhYWEwHhcNMTkwOTE2MjIxNjM3WhcN\n" +
                "MjkwOTEzMjIxNjM3WjApMQswCQYDVQQGEwJhYTELMAkGA1UECAwCYWExDTALBgNV\n" +
                "BAMMBGFhYWEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCqbI/jHW4y\n" +
                "/ne5biCD2dhb7HsWbCStqn1VB4/UOaAXZV2v8Bo+FyQZBo641tCqES6W+P4cZMmc\n" +
                "8URmZF2YTw6PfuangbRW2OcFt1H/l26gcmX2tNnwBxd3WicrYG+jk2vrmO8Ds3Ac\n" +
                "H25MdNIgp8FSn/BrlcGTut8Zou35qrG/xFPXTX3CBkGUIfk3TUkLV/WdgMFFJOPK\n" +
                "Z09ZaSZn/7/sFVD9b32A9UEZ86RpAPE4dVggEzrpjPdqE/Rhbn8yfRmQm9OXYE9Z\n" +
                "9oXgz50rer/w03isis6SYS0KlFxMl8hspWZRYSk9V/Hjl9jZj3/7tdEN84jEHsCX\n" +
                "/glcBAxsJmGtAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAHIluCHkjxoFWKKD6786\n" +
                "oDMfol6CbcZVR3l277EF7B7Wi/5ozyDn0vfjzTsfuTWcntnZ06F6yk97PnW5ed2L\n" +
                "i/IjlzC+AXkg+8Wl7GoTUrgS1cyUsyMK6vGFdB5rQXJP0LC+DjWz1p3XCxRo/3pL\n" +
                "xOHBsp+AlUFfD8ufpDMP3uHjyvbVWzGM0wTEsyjfrMMc71A37WA26DX1EhY/8GQb\n" +
                "miUbAAY38k7dtlZhHgsdQ8ubXghvertG7e6PMf4dyiTZwdsOZIm26Mgj2ZDvA0GA\n" +
                "f0m/ruxRGP4lS9vP6KzSGfo24gM2Lc+ur+PpWBeXhagzSUu854AQUhjuNAbCk1SC\n" +
                "HV4=\n" +
                "-----END CERTIFICATE-----").getBytes()));
        when(samlConfigService.get(eq("test"), eq("1"))).thenReturn(Optional.empty());
        MvcResult mvcResult = mvc.perform(asyncDispatch(mvc.perform(put("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "a")
                .content("{\"idp_sso_url\":\"asd.com\",\"idp_id\":\"test1\"," +
                        "\"idp_cert\": \"" + idpCert + "\",\"enabled\":false, \"id\":\"2\"}"))
                .andReturn()))
                .andExpect(status().isAccepted())
                .andReturn();
        mvcResult.getResponse();
        verify(samlConfigService, times(1)).get("test", "1");
        //id has to be 1 because we are forcing id to be only 1 as there can only be 1 saml config today.
        verify(samlConfigService, times(1)).insert("test",
                SamlConfig.builder().id("1").idpSsoUrl("asd.com").idpCert(idpCert)
                        .enabled(Boolean.FALSE).idpId("test1").build());
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "a")
                .content("{\"idp_sso_url\":\"asd.com\",\"idp_id\":\"test1\"," +
                        "\"idp_cert\": \"" + idpCert + "\",\"enabled\":false}"))
                .andReturn()))
                .andExpect(status().isAccepted())
                .andReturn();
        verify(activityLogService, times(2))
                .insert(eq("test"), any());
    }

    @Test
    public void testUpdateSamlConfig() throws Exception {
        reset(samlConfigService);
        reset(activityLogService);
        String idpCert = new String(Base64.getEncoder().encode(("-----BEGIN CERTIFICATE-----\n" +
                "MIICzjCCAbYCCQCiEaX6dJasBTANBgkqhkiG9w0BAQsFADApMQswCQYDVQQGEwJh\n" +
                "YTELMAkGA1UECAwCYWExDTALBgNVBAMMBGFhYWEwHhcNMTkwOTE2MjIxNjM3WhcN\n" +
                "MjkwOTEzMjIxNjM3WjApMQswCQYDVQQGEwJhYTELMAkGA1UECAwCYWExDTALBgNV\n" +
                "BAMMBGFhYWEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCqbI/jHW4y\n" +
                "/ne5biCD2dhb7HsWbCStqn1VB4/UOaAXZV2v8Bo+FyQZBo641tCqES6W+P4cZMmc\n" +
                "8URmZF2YTw6PfuangbRW2OcFt1H/l26gcmX2tNnwBxd3WicrYG+jk2vrmO8Ds3Ac\n" +
                "H25MdNIgp8FSn/BrlcGTut8Zou35qrG/xFPXTX3CBkGUIfk3TUkLV/WdgMFFJOPK\n" +
                "Z09ZaSZn/7/sFVD9b32A9UEZ86RpAPE4dVggEzrpjPdqE/Rhbn8yfRmQm9OXYE9Z\n" +
                "9oXgz50rer/w03isis6SYS0KlFxMl8hspWZRYSk9V/Hjl9jZj3/7tdEN84jEHsCX\n" +
                "/glcBAxsJmGtAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAHIluCHkjxoFWKKD6786\n" +
                "oDMfol6CbcZVR3l277EF7B7Wi/5ozyDn0vfjzTsfuTWcntnZ06F6yk97PnW5ed2L\n" +
                "i/IjlzC+AXkg+8Wl7GoTUrgS1cyUsyMK6vGFdB5rQXJP0LC+DjWz1p3XCxRo/3pL\n" +
                "xOHBsp+AlUFfD8ufpDMP3uHjyvbVWzGM0wTEsyjfrMMc71A37WA26DX1EhY/8GQb\n" +
                "miUbAAY38k7dtlZhHgsdQ8ubXghvertG7e6PMf4dyiTZwdsOZIm26Mgj2ZDvA0GA\n" +
                "f0m/ruxRGP4lS9vP6KzSGfo24gM2Lc+ur+PpWBeXhagzSUu854AQUhjuNAbCk1SC\n" +
                "HV4=\n" +
                "-----END CERTIFICATE-----").getBytes()));
        when(samlConfigService.get(eq("test"), eq("1"))).thenReturn(
                Optional.of(SamlConfig.builder().id("1").idpSsoUrl("asd.com").idpCert(idpCert)
                        .enabled(Boolean.FALSE).idpId("test1").build()));
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "a")
                .content("{\"idp_sso_url\":\"asd.com\",\"idp_id\":\"test1\"," +
                        "\"idp_cert\":\"" + idpCert + "\",\"enabled\":false}"))
                .andReturn()))
                .andExpect(status().isAccepted());
        mvc.perform(asyncDispatch(mvc.perform(put("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "a")
                .content("{}"))
                .andReturn()))
                .andExpect(status().isBadRequest());
        verify(activityLogService, times(1))
                .insert(eq("test"), any());
    }

    @Test
    public void testDeleteSamlConfig() throws Exception {
        reset(samlConfigService);
        reset(activityLogService);
        mvc.perform(asyncDispatch(mvc.perform(delete("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("session_user", "a")
                .sessionAttr("company", "test"))
                .andReturn()))
                .andExpect(status().isOk());
        verify(samlConfigService, times(1)).delete("test", "1");
        mvc.perform(delete("/v1/samlconfig/1").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "a"))
                .andExpect(status().isNotFound());
        verify(activityLogService, times(1))
                .insert(eq("test"), any());
    }

    @Test
    public void testGetSamlConfig() throws Exception {
        reset(samlConfigService);
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test"))
                .andReturn()))
                .andExpect(status().isOk());
        when(samlConfigService.get("test", "1")).thenReturn(Optional.of(SamlConfig.builder().build()));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/samlconfig").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test"))
                .andReturn()))
                .andExpect(content().json(objectMapper.writeValueAsString(new SamlConfigResponse(SamlConfig.builder().build(),
                        "dGVzdA==","askdj", "askjdh"))))
                .andExpect(status().isOk());
        verify(samlConfigService, times(2)).get("test", "1");
        mvc.perform(get("/v1/samlconfig/1").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test"))
                .andExpect(status().isNotFound());
    }
}
