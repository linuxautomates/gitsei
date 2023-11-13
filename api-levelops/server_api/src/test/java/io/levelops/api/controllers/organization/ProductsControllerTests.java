package io.levelops.api.controllers.organization;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class ProductsControllerTests {
    private MockMvc mvc;
    private OrgProductsController productsController;
    @Autowired
    private ProductsDatabaseService productsService;
    @Autowired
    private IntegrationService integrationService;

    @Before
    public void setup(){
        productsController = new OrgProductsController(productsService, integrationService);
        mvc = MockMvcBuilders.standaloneSetup(productsController).build();
    }

    @Test
    public void test() throws Exception{
        when(productsService.insert(anyString(), any())).thenReturn(UUID.randomUUID().toString());
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/org_products")
                .sessionAttr("session_user", "test@test.com")
                .sessionAttr("company", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"integ1\",\"description\":\"test1\",\"integrations\":{}}"))
                .andReturn()))
            .andExpect(status().is(201))
            .andReturn();
        
        when(productsService.filter(anyString(), any(), anyInt(), anyInt())).thenReturn(DbListResponse.of(List.of(), 0));
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/org_products/list")
                .sessionAttr("session_user", "test@test.com")
                .sessionAttr("company", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page\":\"0\",\"page_size\":\"10\"}"))
                .andReturn()))
            .andExpect(status().is(200))
            .andReturn();
    }
}
