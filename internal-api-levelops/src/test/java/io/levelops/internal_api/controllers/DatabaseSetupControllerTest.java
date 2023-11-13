package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.BootstrapService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.TenantService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class DatabaseSetupControllerTest {

    private static final String TENANT = "test1234567";
    private static final String TENANT_NAME = "Test";

    private MockMvc mvc;
    private DatabaseSchemaService databaseSchemaService;
    private TenantService tenantService;

    private MockedDbService1 service1;
    private MockedDbService2 service2;
    private MockedDbService3 service3;
    private MockedDbService4 service4;

    // creating custom tables since we cannot mock getClass()
    static abstract class MockedDbService1 extends DatabaseService<String> {
        public MockedDbService1(DataSource ds) {
            super(ds);
        }
    }

    static abstract class MockedDbService2 extends DatabaseService<String> {
        public MockedDbService2(DataSource ds) {
            super(ds);
        }
    }

    static abstract class MockedDbService3 extends DatabaseService<String> {
        public MockedDbService3(DataSource ds) {
            super(ds);
        }
    }

    static abstract class MockedDbService4 extends DatabaseService<String> {
        public MockedDbService4(DataSource ds) {
            super(ds);
        }
    }

    @Before
    public void setup() {
        // 3 tenant tables: 1 has no dependencies, 2 depends on 1 and 3 depends on 1 and 2
        service1 = Mockito.mock(MockedDbService1.class);
        when(service1.isTenantSpecific()).thenReturn(true);
        when(service1.getSchemaType()).thenReturn(DatabaseService.SchemaType.TENANT_SPECIFIC);
        when(service1.getReferences()).thenReturn(Set.of());

        service2 = Mockito.mock(MockedDbService2.class);
        when(service2.isTenantSpecific()).thenReturn(true);
        when(service2.getSchemaType()).thenReturn(DatabaseService.SchemaType.TENANT_SPECIFIC);
        when(service2.getReferences()).thenReturn(Set.of(service1.getClass()));

        service3 = Mockito.mock(MockedDbService3.class);
        when(service3.isTenantSpecific()).thenReturn(true);
        when(service3.getSchemaType()).thenReturn(DatabaseService.SchemaType.TENANT_SPECIFIC);
        when(service3.getReferences()).thenReturn(Set.of(service1.getClass(), service2.getClass()));

        // global table
        service4 = Mockito.mock(MockedDbService4.class);
        when(service4.isTenantSpecific()).thenReturn(false);
        when(service4.getSchemaType()).thenReturn(DatabaseService.SchemaType.LEVELOPS_INVENTORY_SCHEMA);

        List<DatabaseService<?>> databaseServiceList = List.of(service1, service2, service3, service4);

        databaseSchemaService = Mockito.mock(DatabaseSchemaService.class);
        tenantService = Mockito.mock(TenantService.class);
        var bootstrapService = Mockito.mock(BootstrapService.class);

        NamedParameterJdbcTemplate template = Mockito.mock(NamedParameterJdbcTemplate.class);
        JdbcOperations jdbcOperations = Mockito.mock(JdbcOperations.class);
        when(template.getJdbcOperations()).thenReturn(jdbcOperations);
        DatabaseSetupController databaseSetupController = new DatabaseSetupController(
            databaseSchemaService,
            tenantService,
            databaseServiceList,
            bootstrapService,
            template);
        mvc = MockMvcBuilders.standaloneSetup(databaseSetupController).build();
    }

    @Test
    public void testEnsureTenantSchemaEndpoint() throws Exception {
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/_ensure_tenant_schema")
                .contentType(MediaType.APPLICATION_JSON)
                .param("company", TENANT)
                .param("company_name", TENANT_NAME)).andReturn()))
                .andExpect(status().isOk());


        verify(tenantService, times(1)).get(eq(null), eq(TENANT));
        verify(tenantService, times(1)).insert(eq(null), eq(Tenant.builder()
                .id(TENANT)
                .tenantName(TENANT_NAME)
                .build()));

        verify(databaseSchemaService, times(1)).ensureSchemaExistence(eq(TENANT));

        verifyEnsureTableExistenceInOrder(TENANT,
                service1,
                service2,
                service3
        );

        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/_ensure_tenant_schema")
                .contentType(MediaType.APPLICATION_JSON)
                .param("company", "test#123")
                .param("company_name", "test")).andReturn()))
                .andExpect(status().isBadRequest());
    }

    @SuppressWarnings("rawtypes")
    private void verifyEnsureTableExistenceInOrder(String tenant, DatabaseService... services) throws SQLException {
        //noinspection RedundantCast (needed to prevent varargs confusion)
        InOrder inOrder = inOrder((Object[]) services);
        for (DatabaseService service : services) {
            inOrder.verify(service, times(1)).ensureTableExistence(eq(tenant));
        }
    }

    @Test
    public void testEnsureInternalSchemaEndpoint() throws Exception {
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/_ensure_internal_schema")
                .contentType(MediaType.APPLICATION_JSON)).andReturn()))
                .andExpect(status().isOk());
        verifyEnsureTableExistenceInOrder(null, service4);
    }
}
