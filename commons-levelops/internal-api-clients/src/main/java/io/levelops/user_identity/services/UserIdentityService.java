package io.levelops.user_identity.services;

import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;

import java.util.Map;

public interface UserIdentityService {

    PaginatedResponse<DbScmUser> listUsers(final String company, final DefaultListRequest filter) throws InternalApiClientException;

}