package io.levelops.auth.utils;

import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.models.DbListResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertThrows;

@RunWith(SpringJUnit4ClassRunner.class)
public class TenantUtilServiceTest {

    @Mock
    private TenantService tenantService;
    @InjectMocks
    private TenantUtilService tenantUtilService;

    @Test
    public void testGetActiveTenants() throws SQLException, ExecutionException {
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());
        List<String> activeTenants = tenantUtilService.getActiveTenants();
        Assert.assertEquals("foo",activeTenants.get(0));
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
    }

    @Test
    public void testGetActiveTenants_cacheCall() throws SQLException, ExecutionException {
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());
        tenantUtilService.getActiveTenants();
        tenantUtilService.getActiveTenants();
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
    }

    @Test
    public void testGetActiveTenants_reloadCache() throws SQLException, ExecutionException {
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());
        tenantUtilService.reloadCache();
        Mockito.verify(tenantService,Mockito.times(2)).list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt());
    }

    @Test
    public void testInvalidTenant() throws SQLException {
        Tenant tenant=new Tenant("foo","foo",System.currentTimeMillis());
        DbListResponse<Tenant> tenants= new DbListResponse<>(1, List.of(tenant),new HashMap<>(),System.currentTimeMillis());
        Mockito.when(tenantService.list(Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt())).thenReturn(tenants).thenReturn(new DbListResponse<>());
        assertThrows(IllegalAccessException.class,()->{tenantUtilService.validateTenant("xyz");});
    }

}
