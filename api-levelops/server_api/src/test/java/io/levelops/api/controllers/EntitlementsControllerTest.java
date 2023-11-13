package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.controllers.EntitlementsController;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.licensing.service.LicensingService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EntitlementsControllerTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String COMPANY = "test";
    private static final String LICENSE = "full";
    private static final License before = License.builder()
            .company(COMPANY).license(LICENSE)
            .entitlements(List.of("e1"))
            .build() ;
    private static final License after = License.builder()
            .company(COMPANY).license(LICENSE)
            .entitlements(List.of("e1", "e2", "e3"))
            .build() ;
    private static final List<String> APPEND_ENTITLEMENTS = List.of("e2", "e3");

    private MockMvc mvc;

    @Mock
    private LicensingService licensingService;

    private EntitlementsController entitlementsController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        entitlementsController = new EntitlementsController(licensingService);
        mvc = MockMvcBuilders
                .standaloneSetup(entitlementsController)
                .build();
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        EntitlementsController.AppendEntitlementsResult expected = EntitlementsController.AppendEntitlementsResult.builder()
                .before(before).after(after)
                .build();
        String str = MAPPER.writeValueAsString(expected);

        EntitlementsController.AppendEntitlementsResult actual = MAPPER.readValue(str, EntitlementsController.AppendEntitlementsResult.class );
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void test() throws Exception {
        Mockito.when(licensingService.getLicense(COMPANY)).thenReturn(before);
        Mockito.when(licensingService.appendEntitlements(COMPANY, before.getEntitlements())).thenReturn(after);

        mvc.perform(asyncDispatch(mvc.perform(patch("/v1/entitlements/append")
                                .contentType(MediaType.APPLICATION_JSON)
                                .sessionAttr("company", COMPANY)
                                .sessionAttr("session_user", "test@harness.io")
                                .content(DefaultObjectMapper.get().writeValueAsString(APPEND_ENTITLEMENTS)))
                        .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));

        verify(licensingService, times(1)).getLicense(eq(COMPANY));
        verify(licensingService, times(1)).appendEntitlements(eq(COMPANY), eq(APPEND_ENTITLEMENTS));
    }
}