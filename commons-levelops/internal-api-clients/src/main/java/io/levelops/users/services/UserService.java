package io.levelops.users.services;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.users.requests.ModifyUserRequest;

import java.io.IOException;

public interface UserService {
    public String createUser(final String company, final ModifyUserRequest rule) throws IOException;

    public User getUser(final String company, final String userId) throws IOException;

    public String updateUser(final String company, String userId, final ModifyUserRequest rule) throws IOException;

    public String deleteUser(final String company, final String userId) throws IOException;

    public String multiUpdateUsers(final String company, final ModifyUserRequest user) throws IOException;

    public DbListResponse<User> listUsers(final String company,
                                          final DefaultListRequest listRequest) throws IOException;

    public DbListResponse<User> listUsers(final String company) throws IOException;
}