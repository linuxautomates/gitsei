package io.levelops.api.controllers.organization;

import io.levelops.api.model.organization.OrgUserDTO;
import io.levelops.api.model.organization.OrgUserDTO.IntegrationUserId;
import io.levelops.api.utils.OrgUserCsvParser;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema.Field.FieldType;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.services.dev_productivity.services.DevProdTaskReschedulingService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import io.propelo.trellis_framework.client.TrellisAPIControllerClient;
import io.propelo.trellis_framework.client.exception.TrellisControllerClientException;
import io.propelo.trellis_framework.models.events.Event;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import io.propelo.trellis_framework.models.events.EventType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/org/users")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class OrgUsersController {
    private final OrgUsersHelper usersHelper;
    private final OrgUsersDatabaseService usersService;
    private final OrgVersionsDatabaseService versionsService;
    private final OrgUserCsvParser csvParser;
    private final DevProdTaskReschedulingService devProdTaskReschedulingService;
    private final TrellisAPIControllerClient trellisAPIControllerClient;
    private final Set<String> persistDevProductivityV2EventsTenantsBlacklist;


    private final OrgUserSchema defaultUserSchema = OrgUserSchema.builder()
            .version(1)
            .createdAt(Instant.now())
            .fields(Set.of(
                    OrgUserSchema.Field.builder()
                            .index(1)
                            .key("email")
                            .displayName("Email")
                            .description("User's email")
                            .type(FieldType.STRING)
                            .systemField(true)
                            .build(),
                    OrgUserSchema.Field.builder()
                            .index(2)
                            .key("full_name")
                            .displayName("Full Name")
                            .description("User's full name")
                            .type(FieldType.STRING)
                            .systemField(true)
                            .build())
            )
            .build();

    @Autowired
    public OrgUsersController(final OrgUsersHelper usersHelper, final OrgUsersDatabaseService usersService, final OrgVersionsDatabaseService versionsService, final OrgUserCsvParser csvParser, DevProdTaskReschedulingService devProdTaskManagementService, TrellisAPIControllerClient trellisAPIControllerClient,
                              @Qualifier("persistDevProductivityV2EventsTenantsBlacklist") Set<String> persistDevProductivityV2EventsTenantsBlacklist) {
        this.usersHelper = usersHelper;
        this.usersService = usersService;
        this.versionsService = versionsService;
        this.csvParser = csvParser;
        this.devProdTaskReschedulingService = devProdTaskManagementService;
        this.trellisAPIControllerClient = trellisAPIControllerClient;
        this.persistDevProductivityV2EventsTenantsBlacklist = persistDevProductivityV2EventsTenantsBlacklist;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUsers(@SessionAttribute(name = "company") final String company, @RequestBody Set<OrgUserDTO> users) {
        return SpringUtils.deferResponse(() -> {
            var prevVersion = versionsService.getActive(company, OrgAssetType.USER).orElse(null);
            var ids = usersHelper.insertNewVersionUsers(company, users.stream()
                    .map(dto -> map(dto)));
            var version = versionsService.getActive(company, OrgAssetType.USER).get();
            persistTrellisOrgUserChangeEvent(company,prevVersion != null ? prevVersion.getVersion() : 0, version.getVersion());
            //reSchdeuleOUOrgUserMappings(company);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", ids, "errors", ""));
        });
    }

    private void reSchdeuleOUOrgUserMappings(final String company) {
        log.info("Rescheduling OU Org User Mappings for company {} starting, trigger users change", company);
        boolean scheduled = devProdTaskReschedulingService.reScheduleOUOrgUserMappingsForOneTenant(company);
        log.info("Rescheduling OU Org User Mappings for company {} completed success {}, trigger users change", company, scheduled);
    }

    private DBOrgUser map(final OrgUserDTO dto) {
        var builder = DBOrgUser.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .ids(dto.getIntegrationUserIds().stream()
                        .map(id -> DBOrgUser.LoginId.builder()
                                .integrationId(Integer.valueOf(id.getIntegrationId()))
                                .cloudId(id.getUserId())
                                .build())
                        .collect(Collectors.toSet()))
                .customFields(dto.getAdditionalFields());
        if (dto.getId() != null) {
            builder.refId(Integer.valueOf(dto.getId()));
        }
        return builder.build();
    }

    private OrgUserDTO map(final DBOrgUser dbUser) {
        return OrgUserDTO.builder()
                .id(String.valueOf(dbUser.getRefId()))
                .orgUUID(dbUser.getId())
                .fullName(dbUser.getFullName())
                .email(dbUser.getEmail())
                .version(dbUser.getVersions().stream().max((v, b) -> v < b ? b : v).get().toString())
                .additionalFields(dbUser.getCustomFields())
                .integrationUserIds(dbUser.getIds().stream()
                        .map(loginId -> IntegrationUserId.builder()
                                .integrationId(String.valueOf(loginId.getIntegrationId()))
                                .userId((String) loginId.getCloudId())
                                .build())
                        .collect(Collectors.toSet()))
                .createdAt(dbUser.getCreatedAt())
                .updatedAt(dbUser.getUpdatedAt())
                .build();
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<OrgUserDTO>> getUser(@SessionAttribute(name = "company") final String company, @PathVariable final UUID id, @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> {
            DBOrgUser dbUser = null;
            if (version != null) {
                dbUser = usersService.get(company, id).get();
            } else {
                dbUser = usersService.filter(company, QueryFilter.builder().strictMatch("version", version).build(), 0, 1).getRecords().get(0);
            }
            return ResponseEntity.ok(map(dbUser));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> listUsers(@SessionAttribute(name = "company") final String company, @RequestBody DefaultListRequest request, @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> {
            QueryFilter filters = QueryFilter.fromRequestFilters(request.getFilter());
            if (version != null) {
                var builder = filters == null ? QueryFilter.builder() : filters.toBuilder();
                filters = builder.strictMatch("version", version).build();
            }
            boolean includeDanglingLogins = request.getFilterValue("include_dangling_logins", Boolean.class)
                    .orElse(true); // true by default to preserve existing behavior
            var listResponse = usersService.filter(company, filters, request.getPage(), request.getPageSize(), includeDanglingLogins);
            return ResponseEntity.ok(Map.of(
                    "_metadata", Map.of(
                            "next_page", request.getPage() + 1,
                            "has_next", listResponse.getTotalCount() > listResponse.getCount(),
                            "page", request.getPage(),
                            "page_size", request.getPageSize(),
                            "total_count", listResponse.getTotalCount()
                    ),
                    "records", listResponse.getRecords().stream()
                            .map(this::map)
                            .collect(Collectors.toList()),

                    "non_users_count", listResponse.getTotals().getOrDefault("dangling", 0)));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> updateUsers(@SessionAttribute(name = "company") final String company, @RequestBody Set<OrgUserDTO> users) {
        return SpringUtils.deferResponse(() -> {
            var prevVersion = versionsService.getActive(company, OrgAssetType.USER).orElse(null);
            usersHelper.updateUsers(company, users.stream().map(item -> map(item)));
            var version = versionsService.getActive(company, OrgAssetType.USER).get();
           // reSchdeuleOUOrgUserMappings(company);
            persistTrellisOrgUserChangeEvent(company,prevVersion != null ? prevVersion.getVersion() : 0, version.getVersion());
            return ResponseEntity.ok("ok");
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> deleteUsers(@SessionAttribute(name = "company") final String company, @RequestBody Set<String> ids) {
        return SpringUtils.deferResponse(() -> {
            var prevVersion = versionsService.getActive(company, OrgAssetType.USER).orElse(null);
            usersHelper.deleteUsers(company, ids.stream().map(Integer::valueOf).collect(Collectors.toSet()));
            var version = versionsService.getActive(company, OrgAssetType.USER).get();
            // reSchdeuleOUOrgUserMappings(company);
            persistTrellisOrgUserChangeEvent(company,prevVersion != null ? prevVersion.getVersion() : 0, version.getVersion());
            return ResponseEntity.ok("ok");
        });
    }

    @PostMapping(path = "/schema", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUserSchema(@SessionAttribute(name = "company") final String company, @RequestBody OrgUserSchema schema) {
        return SpringUtils.deferResponse(() -> {
            var version = usersService.insertUsersSchema(company, schema.getFields());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("version", version.toString()));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @GetMapping(path = "/schema", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<OrgUserSchema>> getUserSchema(@SessionAttribute(name = "company") final String company, @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(getSchema(company, version)));
    }

    private OrgUserSchema getSchema(final String company, final Integer version) {
        return usersService.getUsersSchemas(company, version).orElseGet(() -> defaultUserSchema);
    }

    @PostMapping(path = "/schema/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgUserSchema>>> listSchema(@SessionAttribute(name = "company") final String company, @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var results = usersService.listUsersSchemas(company).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No versions found"));
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), List.copyOf(results)));
        });
    }

    @PostMapping(path = "/import", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> importUsers(
            @SessionAttribute(name = "company") final String company,
            @RequestParam(name = "type", required = true) final String type,
            @RequestParam(name = "import_mode", required = true) final String importMode,
            @RequestParam(name = "file", required = false) final MultipartFile csv) {
        return SpringUtils.deferResponse(() -> {
            if (!"update".equalsIgnoreCase(importMode) && !"replace".equalsIgnoreCase(importMode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect import_mode '" + importMode + "', the allowed options are 'replace' or 'update'");
            }
            var prevVersion = versionsService.getActive(company, OrgAssetType.USER).orElse(null);
            // store file
            var file = Files.createTempFile(Path.of("/tmp"), "propelo_import_ousers", "csv");
            csv.transferTo(file);
            // get user schema
            var schema = getSchema(company, null);
            // get stream of users
            var stream = csvParser.map(company, new FileInputStream(file.toString()), schema);
            // save users
            if ("update".equalsIgnoreCase(importMode)) {
                usersHelper.updateUsers(company, stream.filter(Objects::nonNull));
            } else if ("replace".equalsIgnoreCase(importMode)) {
                usersHelper.insertNewVersionUsers(company, stream);
            }

            // Files
            // get current active version
            var version = versionsService.getActive(company, OrgAssetType.USER).get();
            persistTrellisOrgUserChangeEvent(company,prevVersion != null ? prevVersion.getVersion() : 0, version.getVersion());
            //reSchdeuleOUOrgUserMappings(company);
            // send version back;
            return ResponseEntity.ok(Map.of("version", version.getVersion()));
        });
    }

    private void persistTrellisOrgUserChangeEvent(String company, Integer  prevVersion, Integer activeVersion){
        //Persist ORG_USR_CHANGE event with metadata. Processing to be done in event processor
        if (! persistDevProductivityV2EventsTenantsBlacklist.contains(company)) {
            log.info("Trellis persist V2 event = true, company {}", company);
            try {
                trellisAPIControllerClient.createEvent(Event.builder()
                        .tenantId(company)
                        .eventType(EventType.ORG_USER_CHANGE)
                        .status(JobStatus.SCHEDULED)
                        .data(Map.of("active_version", activeVersion,"prev_version", prevVersion))
                        .build());
            } catch (TrellisControllerClientException e) {
                log.error("Error creating ORG_USER_CHANGE event for version {} ",activeVersion);
            }
        } else {
            log.info("Trellis persist V2 event = false, company {}", company);
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/values", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> getValues(
            @SessionAttribute(name = "company") final String company,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> {
            QueryFilter filters = QueryFilter.fromRequestFilters(request.getFilter());
            if (version != null) {
                filters = filters.toBuilder().strictMatch("version", version).build();
            }
            final var f = filters;
            var results = new ArrayList<Map<String, Object>>();
            var count = new AtomicInteger(0);
            request.getFields().stream().forEach(field -> {
                try {
                    var listResponse = usersService.getValues(company, field, f, request.getPage(), request.getPageSize());
                    results.add(Map.of(field, listResponse));
                    if (count.get() < listResponse.getTotalCount()) {
                        count.set(listResponse.getTotalCount());
                    }
                } catch (SQLException e) {
                    log.error("[{}] unable to get the values for the field '{}': filters={}", company, field, f, e);
                }
            });
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    count.get(),
                    results));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @GetMapping(path = "/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgVersion>>> getVersions(@SessionAttribute(name = "company") final String company, @RequestParam(name = "page_size", defaultValue = "50") Integer pageSize, @RequestParam(name = "page", defaultValue = "0") Integer page) {
        return SpringUtils.deferResponse(() -> {
            var dbResponse = versionsService.filter(company, OrgAssetType.USER, page, pageSize);
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize, dbResponse));
        });
    }

    @PostMapping(path = "/versions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> setActiveVersion(@SessionAttribute(name = "company") final String company, @RequestBody Map<String, Object> request) {
        return SpringUtils.deferResponse(() -> {
            try {
                var prevVersion = versionsService.getActive(company, OrgAssetType.USER).orElse(null);
                usersHelper.activateVersion(company, Integer.valueOf(request.get("active_version").toString()));
                var version = versionsService.getActive(company, OrgAssetType.USER).get();
                persistTrellisOrgUserChangeEvent(company,prevVersion != null ? prevVersion.getVersion() : 0, version.getVersion());
                //reSchdeuleOUOrgUserMappings(company);
                return ResponseEntity.ok("ok");
            } catch (NumberFormatException | SQLException e) {
                log.error("[{}] Unable to activate the version '{}'", company, request.get("active_version").toString(), e);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to activate the version '" + request.get("active_version").toString() + "'. Please make sure the version number is correct and try again or contact support.");
            }
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @GetMapping(path = "/contributor_roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<List<String>>> getContributorRoles(@SessionAttribute(name = "company") final String company) {
        return SpringUtils.deferResponse(() -> {
            var contributorRoles = usersService.getAllContributorRoles(company);
            return ResponseEntity.ok(contributorRoles);
        });
    }
}
