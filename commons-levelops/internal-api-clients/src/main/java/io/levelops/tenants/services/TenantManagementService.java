package io.levelops.tenants.services;

import io.levelops.commons.databases.models.database.TenantState;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.InternalApiClientException;

import java.util.Map;

public interface TenantManagementService {
    public Map<String,String> createTenantState(String company, TenantState state) throws InternalApiClientException;
    public DbListResponse<TenantState> getTenantStates(String company) throws InternalApiClientException;
}
