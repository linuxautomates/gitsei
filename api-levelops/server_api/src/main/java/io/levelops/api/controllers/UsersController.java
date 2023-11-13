package io.levelops.api.controllers;

import io.levelops.api.model.UserMeDto;
import io.levelops.api.services.UserClientHelperService;
import io.levelops.api.services.dev_productivity.DevProductivityOpsService;
import io.levelops.auth.controllers.MFAController;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.SecretsManagerServiceClient.KeyValue;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.users.clients.UsersRESTClient;
import io.levelops.users.requests.ModifyUserRequest;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/users")
@Log4j2
public class UsersController {
    private static final String ACTIVITY_LOG_TEXT = "%s User: %s.";

    private final UserService userService;
    private final LicensingService licensingService;
    private final UsersRESTClient usersRestClient;
    private final ActivityLogService activityLogService;
    private final TenantConfigService tenantConfigService;
    private final UserClientHelperService userClientHelperService;
    private final SecretsManagerServiceClient secretsManagerServiceClient;

    private final DevProductivityOpsService devProductivityOpsService;
    private final OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;

    @Autowired
    public UsersController(UserService userService,
                           ActivityLogService activityLogService,
                           TenantConfigService tenantConfigService,
                           UsersRESTClient usersRestClient,
                           UserClientHelperService userClientHelperService,
                           final SecretsManagerServiceClient secretsManagerServiceClient,
                           final LicensingService licensingService, DevProductivityOpsService devProductivityOpsService, OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService) {
        this.userService = userService;
        this.usersRestClient = usersRestClient;
        this.activityLogService = activityLogService;
        this.tenantConfigService = tenantConfigService;
        this.userClientHelperService = userClientHelperService;
        this.secretsManagerServiceClient = secretsManagerServiceClient;
        this.licensingService = licensingService;
        this.devProductivityOpsService = devProductivityOpsService;
        this.orgUnitCategoryDatabaseService = orgUnitCategoryDatabaseService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createUser(@RequestBody final ModifyUserRequest request,
                                                                          @SessionAttribute(name = "session_user") final String sessionUser,
                                                                          @SessionAttribute(name = "company") final String company) {
        return SpringUtils.deferResponse(() -> {
            final User finalUser = userClientHelperService.createUser(company, sessionUser, request);
            return ResponseEntity.ok(Map.of("id", finalUser.getId()));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> userUpdate(final @RequestBody ModifyUserRequest request,
                                                              final @PathVariable("userid") String userId,
                                                              final @SessionAttribute(name = "session_user") String sessionUser,
                                                              final @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            // check if user is trying to disable MFA but Global MFA is enforced
            var user = usersRestClient.getUser(company, userId);
            var status = updateUser(company, user, request, sessionUser);
            return ResponseEntity.ok(status);
        });
    }

    private Boolean updateUser(final String company, final User user, ModifyUserRequest request, final String sessionUser) throws IOException, SQLException {
        var key = MFAController.getSecretsManagerKey(company, user.getEmail());
        var secret = "";
        Boolean mfaEnabled = request.getMfaEnabled();
        Long mfaEnrollmentWindowExpiry = request.getMfaEnrollmentWindowExpiry();
        Long mfaResetAt = request.getMfaResetAt();
        if (BooleanUtils.isFalse(mfaEnabled)) {
            boolean globalMfaEnforcement = BooleanUtils.isTrue(MFAController.isMFAEnforced(tenantConfigService, company));
            // if global enforcement is set and the enrollment window is not getting updated (trying to simply disable MFA) then return error.
            if (globalMfaEnforcement && mfaEnrollmentWindowExpiry == null) {
                log.warn("MFA is globally enforced. Not letting user disable MFA: company={}, user={}, targetUser={}", company, sessionUser, user.getEmail());
                // setting to null so that existing value is unchanged:
                mfaEnabled = null;
                mfaResetAt = null;
            } else {
                // remove secret
                secret = MFAController.getMFASecret(secretsManagerServiceClient, company, user.getEmail());
                MFAController.deleteMFASecret(secretsManagerServiceClient, company, user.getEmail());
            }
        }

        request = new ModifyUserRequest(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getUserType(),
                request.getSamlAuthEnabled(),
                request.getPasswordAuthEnabled(),
                request.getNotifyUser(),
                mfaEnabled,
                request.getMetadata(),
                mfaEnrollmentWindowExpiry,
                mfaResetAt,
                request.getManagedOURefIds());

        Boolean status = Boolean.TRUE.equals(Boolean.valueOf(usersRestClient.updateUser(company, user.getId(), request)));
        if (status) {
            User finalUser = usersRestClient.getUser(company, user.getId());
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(user.getId())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.USER)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited", user.getEmail()))
                    .details(Collections.singletonMap("item", sanitizeUser(finalUser)))
                    .action(ActivityLog.Action.EDITED)
                    .build());
        } else if (Strings.isNotBlank(secret)) {
            try {
                secretsManagerServiceClient.storeKeyValue(company, SecretsManagerServiceClient.DEFAULT_CONFIG_ID, KeyValue.builder().key(key).value(secret).build());
            } catch (SecretsManagerServiceClientException e) {
                log.error("[{}] Unable to restore MFA secret for user '{}'. The login might be broken", company, user.getId(), e);
            }
        }
        return status;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> userDelete(@SessionAttribute(name = "company") String company,
                                                           @SessionAttribute(name = "session_user") String sessionUser,
                                                           @PathVariable("userid") String userId) {
        return SpringUtils.deferResponse(() -> {
            var user = usersRestClient.getUser(company, userId);
            usersRestClient.deleteUser(company, userId);
            MFAController.deleteMFASecret(secretsManagerServiceClient, company, user.getEmail());
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(userId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.USER)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", user.getEmail()))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok().build();
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/multi_update", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> multiUpdateUsers(@RequestBody ModifyUserRequest request,
                                                                 @SessionAttribute(name = "session_user") String sessionUser,
                                                                 @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            usersRestClient.multiUpdateUsers(company, request);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem("multi-users")
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.USER)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Multi Update", "Users"))
                    .details(Collections.singletonMap("update", request))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            return ResponseEntity.ok().build();
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LIMITED_USER', 'AUDITOR', 'RESTRICTED_USER', 'ASSIGNED_ISSUES_USER','PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/me", produces = "application/json")
    public DeferredResult<ResponseEntity<UserMeDto>> getSessionUser(@SessionAttribute(name = "company") String company,
                                                                    @SessionAttribute(name = "session_user") String username) {
        return SpringUtils.deferResponse(() -> {
            var mfaEnforced = MFAController.isMFAEnforced(tenantConfigService, company);
            log.info("username is {}, company is {}", username, company);
            User user = userService.getByEmail(company, username).orElseThrow();
            TenantConfig config = tenantConfigService
                    .listByFilter(company, user.getUserType().toString() + "_LANDING_PAGE",
                            0, 1)
                    .getRecords()
                    .stream()
                    .findFirst()
                    .orElse(TenantConfig.builder().build());

            var license = licensingService.getLicense(company);
            UserMeDto userDTO = UserMeDto.builder()
                    .metadata(user.getMetadata())
                    .user(sanitizeUser(user).toBuilder()
                            .mfaEnforced(mfaEnforced)
                            .build())
                    .landingPage(config.getValue())
                    .entitlements(Set.copyOf(license.getEntitlements()))
                    .license(license.getLicense())
                    .build();
            return ResponseEntity.ok(userDTO);
        });
    }


    @RequestMapping(method = RequestMethod.GET, value = "/entitlements", produces = "application/json")
    public ResponseEntity<Set<String>> getEntitlements(@SessionAttribute(name = "defaultEntitlements") Set<String> defaultEntitlements) {
            return ResponseEntity.ok(defaultEntitlements);
    }

    private ModifyUserRequest sanitizeUpdateUserDetailsRequest(ModifyUserRequest request) {
        final ModifyUserRequest sanitizedRequest = new ModifyUserRequest(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                null, //In User Detail update - changing user type is not permitted
                request.getSamlAuthEnabled(),
                request.getPasswordAuthEnabled(),
                request.getNotifyUser(),
                request.getMfaEnabled(),
                request.getMetadata(),
                request.getMfaEnrollmentWindowExpiry(),
                request.getMfaResetAt(),
                request.getManagedOURefIds());
        log.info("request = {}, sanitizedRequest = {}", request, sanitizedRequest);
        return sanitizedRequest;
    }


    @PreAuthorize("hasAnyAuthority('ADMIN', 'LIMITED_USER', 'AUDITOR', 'RESTRICTED_USER', 'ASSIGNED_ISSUES_USER','PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT, value = "/me/details", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> sessionUserUpdate(final @RequestBody ModifyUserRequest request,
                                                                     final @SessionAttribute(name = "session_user") String sessionUser,
                                                                     final @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<User> userList = usersRestClient.listUsers(company, DefaultListRequest.builder()
                    .filter(Map.of("partial", Map.of("email", sessionUser)))
                    .build());
            if (userList.getTotalCount() != 1 || !userList.getRecords().get(0).getEmail().equalsIgnoreCase(sessionUser)) {
                log.error("[{}] Unable to retrieve the session user's details ('{}') to perform an update. results: {}", company, sessionUser, userList);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to update the current user. please try again in a few minutes");
            }
            User user = sanitizeUser(userList.getRecords().get(0));
            final ModifyUserRequest sanitizeRequest = sanitizeUpdateUserDetailsRequest(request);
            return ResponseEntity.ok(updateUser(company, user, sanitizeRequest, sessionUser));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LIMITED_USER', 'AUDITOR', 'RESTRICTED_USER', 'ASSIGNED_ISSUES_USER','PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/me/details", produces = "application/json")
    public DeferredResult<ResponseEntity<User>> sessionUserDetails(@SessionAttribute(name = "company") String company,
                                                                   final @SessionAttribute(name = "session_user") String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            var userList = usersRestClient.listUsers(company, DefaultListRequest.builder().filter(Map.of("partial", Map.of("email", sessionUser))).build());
            if (userList.getTotalCount() != 1 || !userList.getRecords().get(0).getEmail().equalsIgnoreCase(sessionUser)) {
                log.error("[{}] Unable to retreive the session user's details ('{}')", company, sessionUser);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to locate the current user. please try again in a few minutes");
            }
            var user = userList.getRecords().get(0);
            user = populateWorkspaceOuMappingsInMetadata(company, user);
            return ResponseEntity.ok(sanitizeUser(user).toBuilder()
                    .mfaEnforced(MFAController.isMFAEnforced(tenantConfigService, company))
                    .build());
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, value = "/{userid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<User>> userDetails(@SessionAttribute(name = "company") String company,
                                                            @PathVariable("userid") String userId) {
        return SpringUtils.deferResponse(() -> {
            var mfaEnforced = MFAController.isMFAEnforced(tenantConfigService, company);
            return ResponseEntity.ok(sanitizeUser(populateWorkspaceOuMappingsInMetadata(company, usersRestClient.getUser(company, userId)).toBuilder()
                    .mfaEnforced(mfaEnforced)
                    .build()));
        });
    }

    //SEI-1779 : This is a temporary fix and should be removed later
    private User populateWorkspaceOuMappingsInMetadata(String company, User user) throws SQLException {
        Map<String, Object> metadata = user.getMetadata();
        if (CollectionUtils.isNotEmpty(user.getManagedOURefIds())) {
            Map<String, List<String>> workspaceOuMappings = orgUnitCategoryDatabaseService.getWorkspaceOuRefIdMappingsByOuRefIds(company, user.getManagedOURefIds());
            metadata.put("workspaces", workspaceOuMappings);
            user = user.toBuilder().metadata(metadata).build();
        }
        return user;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<User>>> usersList(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(0,
                        DefaultListRequest.DEFAULT_PAGE_SIZE,
                        sanitizeUsers(usersRestClient.listUsers(company)))));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<User>>> usersListFiltered(@SessionAttribute(name = "company") String company,
                                                                                     @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(
                        filter.getPage(),
                        filter.getPageSize(),
                        sanitizeUsers(usersRestClient.listUsers(company, filter)))));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/trellis/assign-scopes", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<User>>> assignScopesForUser(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("assignScopesForUser company = {}, requestorEmail = {}, scopes = {}, request = {}", company, requestorEmail, scopes, request);
            List<User> updatedUsers = devProductivityOpsService.assignDevProdScope(company, requestorEmail, scopes, request);
            return ResponseEntity.ok().body(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    sanitizeUsers(updatedUsers)
            ));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/trellis/remove-scopes", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<User>>> removeScopesForUser(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("removeScopesForUser company = {}, requestorEmail = {}, scopes = {}, request = {}", company, requestorEmail, scopes, request);
            List<User> updatedUsers = devProductivityOpsService.removeDevProdScope(company, requestorEmail, scopes, request);
            return ResponseEntity.ok().body(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    sanitizeUsers(updatedUsers)
            ));
        });
    }

    private static DbListResponse<User> sanitizeUsers(DbListResponse<User> list) {
        if (list == null) {
            return null;
        }
        return DbListResponse.of(sanitizeUsers(list.getRecords()), list.getTotalCount());
    }

    private static List<User> sanitizeUsers(List<User> list) {
        return ListUtils.emptyIfNull(list).stream()
                .map(UsersController::sanitizeUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static User sanitizeUser(User u) {
        if (u == null) {
            return null;
        }
        return u.toBuilder()
                .passwordResetDetails(null)
                .bcryptPassword(null)
                .build();
    }

}
