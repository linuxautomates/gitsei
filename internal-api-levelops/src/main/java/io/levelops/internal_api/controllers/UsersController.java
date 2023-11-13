package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.internal_api.converters.ModifyUserRequestToUserConverter;
import io.levelops.users.requests.ModifyUserRequest;
import io.levelops.web.util.SpringUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/users")
public class UsersController {

    private final UserService userService;
    private final ModifyUserRequestToUserConverter converter;

    @Autowired
    public UsersController(UserService userService,
                           ModifyUserRequestToUserConverter converter) {
        this.converter = converter;
        this.userService = userService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUser(@RequestBody ModifyUserRequest request,
                                                                          @PathVariable(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(request.getEmail()) || StringUtils.isEmpty(request.getFirstName())
                    || request.getSamlAuthEnabled() == null || request.getPasswordAuthEnabled() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not all required fields present.");
            }
            if (!request.getSamlAuthEnabled() && !request.getPasswordAuthEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both auth flows cannot be disabled.");
            }
            if (userService.getByEmail(company,request.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
            }

            ImmutablePair<User, String> userResetPair = converter.convertNewUserRequest(request);
            String userId = userService.insert(company, userResetPair.getLeft());
            return ResponseEntity.ok(Map.of("id", userId,
                    "reset_token", userResetPair.right));
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> userUpdate(@RequestBody ModifyUserRequest request,
                                                              @PathVariable("userid") String userId,
                                                              @PathVariable(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(request.getEmail()) && StringUtils.isEmpty(request.getFirstName())
                    && request.getLastName() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user data to update.");
            }
            //if we dont force the presence of both fields, we will have to read from db everytime
            if (request.getSamlAuthEnabled() == null || request.getPasswordAuthEnabled() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "saml_auth_enabled and password_auth_enabled should always be provided.");
            }
            if (!request.getSamlAuthEnabled() && !request.getPasswordAuthEnabled()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both auth flows cannot be disabled.");
            }
            Boolean status = userService.update(company, converter.convertUpdateRequest(request, userId));
            return ResponseEntity.ok(status);
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> userDelete(@PathVariable(name = "company") String company,
                                                           @PathVariable("userid") String userId) {
        return SpringUtils.deferResponse(() -> {
            userService.delete(company, userId);
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<User>> userDetails(@PathVariable(name = "company") String company,
                                                            @PathVariable("userid") String userId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(userService.get(company, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."))));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<User>>> usersList(@PathVariable(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(userService.list(company, 0,
                DefaultListRequest.DEFAULT_PAGE_SIZE)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/multi_update", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> updateMultipleUsers(@PathVariable(name = "company") String company,
                                                                    @RequestBody ModifyUserRequest updateFieldAndValues) {
        return SpringUtils.deferResponse(() -> {
            userService.multiUpdatePasswordOrSaml(company, RoleType.fromString(updateFieldAndValues.getUserType()),
                    updateFieldAndValues.getPasswordAuthEnabled(), updateFieldAndValues.getSamlAuthEnabled());
            return ResponseEntity.ok().build();
        });
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<User>>> usersListFiltered(@PathVariable(name = "company") String company,
                                                                                  @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            RoleType userType = filter.getFilterValue("user_type", String.class)
                    .map(RoleType::fromString)
                    .orElse(null);
            List<String> userIds = filter.<String>getFilterValueAsList("ids")
                        .or(() -> filter.getFilterValueAsList("_ids"))
                        .orElse(null);
            List<String> managedOuRefIds = filter.<String>getFilterValueAsList("managed_ou_ref_ids")
                    .orElse(null);
            String partialEmail = filter.getFilterValueAsMap("partial")
                        .map(map -> (String) map.get("email"))
                        .orElse(null);

            Map<String, Integer> updateRange = filter.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;

            DbListResponse<User> users = userService.listByFilters(company, userIds, partialEmail, userType,
                    updatedAtStart, updatedAtEnd, managedOuRefIds, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(users);
        });
    }

}
