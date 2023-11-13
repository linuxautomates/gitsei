package io.levelops.dashboard.services;

import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.FileUpload;
import io.levelops.exceptions.InternalApiClientException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {

    Map<String,String> createDashboard(final String company, final Dashboard dashboard) throws InternalApiClientException;

}