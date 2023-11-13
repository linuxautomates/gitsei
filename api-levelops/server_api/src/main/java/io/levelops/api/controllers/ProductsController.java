package io.levelops.api.controllers;

import com.amazonaws.services.pinpointsmsvoicev2.model.ValidationException;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityParentProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import io.levelops.workitems.clients.WorkItemsClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;

@RestController
@Log4j2
@RequestMapping({"/v1/products", "/v1/org/workspaces"})
public class ProductsController {
    private static final String ACTIVITY_LOG_TEXT = "%s Products item: %s.";
    private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_][a-zA-Z0-9\\-_\\s]*[a-zA-Z0-9\\-_]$");
    private static final Pattern PRODUCT_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]*$");
    private static final Pattern FOREIGN_KEY_VIOLATION = Pattern.compile("^.*violates foreign key constraint.*on table \\\"(?<tblname>.*)\\\".*$", Pattern.MULTILINE);
    private static final Pattern DUPLICATE_KEY_ERROR = Pattern.compile("Duplicate key exists");
    private static final Pattern DUPLICATE_NAME_ERROR = Pattern.compile("Duplicate name exists");
    private static final Pattern DUPLICATE_KEY_VALUE_ERROR = Pattern.compile("ERROR: duplicate key value violates unique constraint \"uniq_products_key_idx\"");
    private static final Pattern DUPLICATE_NAME_VALUE_ERROR = Pattern.compile("ERROR: duplicate key value violates unique constraint \"uniq_products_name_idx\"");
    private static final String DOUBLE_HASH = "##";

    private final WorkItemsClient workItemsClient;
    private final ProductService productService;
    private final ActivityLogService activityLogService;
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final OrgUnitCategoryDatabaseService categoryService;
    private final OrgUnitHelper ouHelper;
    private final OrgUnitsDatabaseService ouService;
    private final DashboardWidgetService dashboardWidgetService;
    private final OUDashboardService ouDashboardService;
    private final TenantConfigService configService;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;
    private final DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService;


    @Value("${LEGACY_REQUEST:true}")
    public boolean legacyRequest;

    @Autowired
    public ProductsController(final ProductService productService, final ActivityLogService logService, final WorkItemsClient workItemsClient,
                              final CiCdJobsDatabaseService ciCdJobsDatabaseService, final OrgUnitCategoryDatabaseService categoryService, final OrgUnitHelper ouHelper,
                              final OrgUnitsDatabaseService ouService, final DashboardWidgetService dashboardWidgetService, final OUDashboardService ouDashboardService, TenantConfigService configService, @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled, @Qualifier("parentProfilesEnabledTenants") Set<String> parentProfilesEnabledTenants, DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService, boolean isLegacy) {
        this.productService = productService;
        this.activityLogService = logService;
        this.workItemsClient = workItemsClient;
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.categoryService = categoryService;
        this.ouHelper = ouHelper;
        this.ouService = ouService;
        this.dashboardWidgetService=dashboardWidgetService;
        this.ouDashboardService=ouDashboardService;
        this.configService = configService;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
        this.devProductivityParentProfileDatabaseService = devProductivityParentProfileDatabaseService;
        this.legacyRequest = isLegacy;
    }

    protected void validateInput(final Product product, final boolean insert) {
        validateInput(product, insert, "");
    }

    protected void validateInput(final Product product, final boolean insert, final String user) {
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace object cannot be null.");
        }
        if (StringUtils.isBlank(product.getName())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace name cannot be null or empty or blank.");
            }
        } else {
            Matcher matcher = PRODUCT_NAME_PATTERN.matcher(product.getName().trim());
            if (!matcher.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace name should contain only \'a-z A-Z 0-9 -_\' or space. Input: " + product.getName());
            }
        }
        if (StringUtils.isBlank(product.getKey())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace key cannot be null or empty or blank.");
            }
        } else {
            Matcher matcher = PRODUCT_KEY_PATTERN.matcher(product.getKey().trim());
            if (!matcher.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace key should contain only \'a-z A-Z 0-9 -_\'. Input: " + product.getKey());
            }
        }
        if (StringUtils.isBlank(product.getOwnerId())) {
            if (legacyRequest && insert && !(Boolean.TRUE.equals(product.getBootstrapped()) && Boolean.TRUE.equals(product.getImmutable()) && user.equalsIgnoreCase("do-not-reply@propelo.ai"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace owner cannot be null or empty or blank.");
            }
        }

        if(!legacyRequest && StringUtils.isBlank(product.getOrgIdentifier())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace orgIdentifier cannot be null or empty or blank.");
            }
        }
        // if no integration is added, we'll let it pass as there will always be a default propelo/dummy integration
        // if(insert && CollectionUtils.isEmpty(product.getIntegrationIds())) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one integration is needed for a Workspace.");
        // }
        return;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createProduct(@RequestBody Product product,
                                                                             @SessionAttribute(name = "session_user") String sessionUser,
                                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                var productId = createWorkspace(product, sessionUser, company);
                return ResponseEntity.ok(Map.of("id", productId));
            } catch (ValidationException ev) {
                log.error(ev.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } catch (Exception e) {
                String msg = e.getMessage();
                Matcher matcher = DUPLICATE_KEY_ERROR.matcher(msg);
                if (matcher.find()) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Key already exists");
                }
                Matcher nameMatcher = DUPLICATE_NAME_ERROR.matcher(msg);
                if (nameMatcher.find()) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Name already exists");
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        });
    }

    public String createWorkspace(Product product, String sessionUser, String company) throws SQLException {
        try {
            validateInput(product, true, sessionUser);
        } catch (Exception e) {
            throw new ValidationException(e.getMessage());
        }

        String productId = productService.insert(company, product);
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(productId)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                .body(String.format(ACTIVITY_LOG_TEXT, "Created", productId))
                .details(Collections.singletonMap("item", product))
                .action(ActivityLog.Action.CREATED)
                .build());
        // create default categories
        var teamsCategory = OrgUnitCategory.builder()
                .name("Teams")
                .description("Teams")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Teams")
                .build();
        var projectsCategory = OrgUnitCategory.builder()
                .name("Projects")
                .description("Projects")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Projects")
                .build();
        var sprintsCategory = OrgUnitCategory.builder()
                .name("Sprints")
                .description("Sprints")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Sprints")
                .build();
        String teamsCatId = categoryService.insert(company, teamsCategory);
        String sprintsCatId = categoryService.insert(company, sprintsCategory);
        String projectsCatId = categoryService.insert(company, projectsCategory);
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            mapRootOUsToCentralTrellisProfile(company, List.of(teamsCatId, sprintsCatId, projectsCatId));
        }
        return productId;
    }

    private void mapRootOUsToCentralTrellisProfile(final String company,List<String> categoryIds) {
        Set<String> rootOuRefIds = null;
        DevProductivityParentProfile centralProfile = null;
        try {
            rootOuRefIds = categoryService.filter(company,QueryFilter.builder().strictMatches(Map.of("id",categoryIds)).build(),0,10).getRecords()
                    .stream().map(OrgUnitCategory::getRootOuRefId).map(String::valueOf).collect(Collectors.toSet());
            centralProfile =devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
            if(centralProfile != null &&CollectionUtils.isNotEmpty(rootOuRefIds))
                devProductivityParentProfileDatabaseService.updateProfileOUMappings(company,centralProfile.getId(), rootOuRefIds);
        } catch (SQLException e) {
            log.error("Unable to update root OU -> central profile mapping ",e);
        }
    }

    public String createDemoWorkspace(String sessionUser, String company) throws SQLException, BadRequestException {

        List<Product> productsList = productService.listByFilter(company,"Initech Demo workspace",null,null,null,0,1).getRecords();
        Product initechWorkspace = null;
        if(CollectionUtils.isNotEmpty(productsList))
            initechWorkspace = productsList.get(0);
        if(initechWorkspace != null){
            throw new BadRequestException("Error creating initech workspace. It already exists!");
        }

        var product = Product.builder()
                .bootstrapped(true)
                .immutable(true)
                .name("Initech Demo workspace")
                .description("Demo Workspace")
                .key("DEMO")
                .demo(true)
                .build();
        try {
            validateInput(product, true, sessionUser);
        } catch (Exception e) {
            throw new ValidationException(e.getMessage());
        }


        String productId = productService.insert(company, product);

        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(productId)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                .body(String.format(ACTIVITY_LOG_TEXT, "Created", productId))
                .details(Collections.singletonMap("item", product))
                .action(ActivityLog.Action.CREATED)
                .build());

        configService.insert(company, TenantConfig.builder().name(product.getName()).value(productId).build());
        // create demo category
        var teamsCategory = OrgUnitCategory.builder()
                .name("Teams")
                .description("Teams")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Teams")
                .build();
        var projectsCategory = OrgUnitCategory.builder()
                .name("Projects")
                .description("Projects")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Projects")
                .build();
        var sprintsCategory = OrgUnitCategory.builder()
                .name("Sprints")
                .description("Sprints")
                .enabled(true)
                .workspaceId(Integer.parseInt(productId))
                .rootOuName("All Sprints")
                .build();
        UUID teamCatId=UUID.fromString(categoryService.insert(company, teamsCategory));
        configService.insert(company, TenantConfig.builder().name(teamsCategory.getName()).value(teamCatId.toString()).build());

        DbListResponse<DBOrgUnit> orgUnits=ouService.getOusForGroupId(company,teamCatId);
        DBOrgUnit allTeamsOU = orgUnits.getRecords().get(0);
        UUID allTeamOuId = allTeamsOU.getId();
        Integer allTeamOuRefId = allTeamsOU.getRefId();
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName())
                .value(allTeamOuRefId.toString())
                .build());
        Integer allTeamAliDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_teams_alignment");
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + "all_teams_alignment")
                .value(allTeamAliDashboardId.toString())
                .build());
        Integer allTeamPlanDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_teams_planning");
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + "all_teams_planning")
                .value(allTeamPlanDashboardId.toString())
                .build());
        Integer allTeamExeDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_teams_dora_metrics");
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + "all_teams_dora_metrics")
                .value(allTeamExeDashboardId.toString())
                .build());
        Integer allTeamTrellisDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_teams_trellis_score");
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + "all_teams_trellis_score")
                .value(allTeamTrellisDashboardId.toString())
                .build());
        Integer allTeamDevDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_teams_dev_insights");
        configService.insert(company, TenantConfig.builder()
                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + "all_teams_dev_insights")
                .value(allTeamDevDashboardId.toString())
                .build());
        List<OUDashboard>allTeamDashboardList=List.of(OUDashboard.builder().dashboardId(allTeamExeDashboardId).dashboardOrder(1).build(),OUDashboard.builder().dashboardId(allTeamAliDashboardId).dashboardOrder(4).build(),
                OUDashboard.builder().dashboardId(allTeamPlanDashboardId).dashboardOrder(3).build(),OUDashboard.builder().dashboardId(allTeamTrellisDashboardId).dashboardOrder(5).build(),OUDashboard.builder().dashboardId(allTeamDevDashboardId).dashboardOrder(2).build());
        ouDashboardService.updateOuMapping(company,allTeamDashboardList,allTeamOuId);
        DBOrgUnit frontendOu=DBOrgUnit.builder().name("Frontend").description("Frontend Demo").ouCategoryId(teamCatId).parentRefId(allTeamOuRefId).build();
        DBOrgUnit backendOu=DBOrgUnit.builder().name("Backend").description("Backend Demo").ouCategoryId(teamCatId).parentRefId(allTeamOuRefId).build();
        DBOrgUnit contractorsOu=DBOrgUnit.builder().name("Contractors").description("Contractors Demo").ouCategoryId(teamCatId).parentRefId(allTeamOuRefId).build();
        Stream<DBOrgUnit> allTeamChildSet=Stream.of(frontendOu,backendOu,contractorsOu);
        ouHelper.insertNewOrgUnits(company, allTeamChildSet);
        DbListResponse<DBOrgUnit> teamOuList=ouService.getOusForGroupId(company,teamCatId);
        teamOuList.getRecords().stream().forEach(unit->{
                if(unit.getParentRefId()!=null){
                    try {
                        configService.insert(company, TenantConfig.builder()
                                .name(teamsCategory.getName() + DOUBLE_HASH + allTeamsOU.getName() + DOUBLE_HASH + unit.getName())
                                .value(String.valueOf(unit.getRefId()))
                                .build());
                    }catch(SQLException e){
                        log.error("[{}] Error occur while inserting into tenant config: {}", company, e);
                    }
                }
                }
        );
        UUID sprintCatId=UUID.fromString(categoryService.insert(company, sprintsCategory));
        configService.insert(company, TenantConfig.builder().name(sprintsCategory.getName()).value(sprintCatId.toString()).build());

        DbListResponse<DBOrgUnit> sprintOrgUnits=ouService.getOusForGroupId(company,sprintCatId);
        DBOrgUnit allSprintsOU = sprintOrgUnits.getRecords().get(0);
        UUID allSprintsOuId = allSprintsOU.getId();
        Integer allSprintsOuRefId = allSprintsOU.getRefId();
        configService.insert(company, TenantConfig.builder()
                .name(sprintsCategory.getName() + DOUBLE_HASH + allSprintsOU.getName())
                .value(allSprintsOuRefId.toString())
                .build());

        Integer allSprintPlanDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_sprints_planning");
        configService.insert(company, TenantConfig.builder()
                .name(sprintsCategory.getName() + DOUBLE_HASH + allSprintsOU.getName() + DOUBLE_HASH + "all_sprints_planning")
                .value(allSprintPlanDashboardId.toString())
                .build());

        List<OUDashboard>allSprintDashboardList=List.of(OUDashboard.builder().dashboardId(allSprintPlanDashboardId).dashboardOrder(1).build());
        ouDashboardService.updateOuMapping(company,allSprintDashboardList,allSprintsOuId);

        DBOrgUnit spTwoThreeOu=DBOrgUnit.builder().name("SP23").description("SP23 Demo").ouCategoryId(sprintCatId).parentRefId(allSprintsOuRefId).build();
        DBOrgUnit spTwoFour=DBOrgUnit.builder().name("SP24").description("SP24 Demo").ouCategoryId(sprintCatId).parentRefId(allSprintsOuRefId).build();
        DBOrgUnit spTwoFiveOu=DBOrgUnit.builder().name("SP25").description("SP25 Demo").ouCategoryId(sprintCatId).parentRefId(allSprintsOuRefId).build();
        Stream<DBOrgUnit> allSprintChildSet=Stream.of(spTwoThreeOu,spTwoFour,spTwoFiveOu);
        ouHelper.insertNewOrgUnits(company, allSprintChildSet);
        DbListResponse<DBOrgUnit> sprintOuList=ouService.getOusForGroupId(company,sprintCatId);
        sprintOuList.getRecords().stream().forEach(unit->{
                    if(unit.getParentRefId()!=null){
                        try {
                            configService.insert(company, TenantConfig.builder()
                                    .name(sprintsCategory.getName() + DOUBLE_HASH + allSprintsOU.getName() + DOUBLE_HASH + unit.getName())
                                    .value(String.valueOf(unit.getRefId()))
                                    .build());
                        }catch(SQLException e){
                            log.error("[{}] Error occur while inserting into tenant config: {}", company, e);
                        }
                    }
                }
        );
        UUID projectCatId=UUID.fromString(categoryService.insert(company, projectsCategory));
        configService.insert(company, TenantConfig.builder().name(projectsCategory.getName()).value(projectCatId.toString()).build());

        DbListResponse<DBOrgUnit> allProjectOrgUnits=ouService.getOusForGroupId(company,projectCatId);
        DBOrgUnit allprojectsOU = allProjectOrgUnits.getRecords().get(0);
        UUID allProjectsOuId = allprojectsOU.getId();
        Integer allProjectsOuRefId = allprojectsOU.getRefId();
        configService.insert(company, TenantConfig.builder()
                .name(projectsCategory.getName() + DOUBLE_HASH + allprojectsOU.getName())
                .value(allProjectsOuRefId.toString())
                .build());

        Integer allProjectAliDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_projects_alignment");
        configService.insert(company, TenantConfig.builder()
                .name(projectsCategory.getName() + DOUBLE_HASH + allprojectsOU.getName() + DOUBLE_HASH + "all_projects_alignment")
                .value(allProjectAliDashboardId.toString())
                .build());

        Integer allProjectPlanDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_projects_planning");
        configService.insert(company, TenantConfig.builder()
                .name(projectsCategory.getName() + DOUBLE_HASH + allprojectsOU.getName() + DOUBLE_HASH + "all_projects_planning")
                .value(allProjectPlanDashboardId.toString())
                .build());

        Integer allProjectExeDashboardId=dashboardWidgetService.createDemoDashboards(company,"all_projects_dora_metrics");
        configService.insert(company, TenantConfig.builder()
                .name(projectsCategory.getName() + DOUBLE_HASH + allprojectsOU.getName() + DOUBLE_HASH + "all_projects_dora_metrics")
                .value(allProjectExeDashboardId.toString())
                .build());

        List<OUDashboard>allProjectDashboardList=List.of(OUDashboard.builder().dashboardId(allProjectExeDashboardId).dashboardOrder(1).build(),OUDashboard.builder().dashboardId(allProjectAliDashboardId).dashboardOrder(3).build(),
                OUDashboard.builder().dashboardId(allProjectPlanDashboardId).dashboardOrder(2).build());
        ouDashboardService.updateOuMapping(company,allProjectDashboardList,allProjectsOuId);

        Integer allProjectOuRefId= allProjectOrgUnits.getRecords().get(0).getRefId();
        DBOrgUnit cloudOu=DBOrgUnit.builder().name("Cloud Transformation").description("Frontend Demo").ouCategoryId(projectCatId).parentRefId(allProjectOuRefId).build();
        DBOrgUnit modernOu=DBOrgUnit.builder().name("Modernization").description("Backend Demo").ouCategoryId(projectCatId).parentRefId(allProjectOuRefId).build();
        DBOrgUnit mobileOu=DBOrgUnit.builder().name("Mobile").description("Contractors Demo").ouCategoryId(projectCatId).parentRefId(allProjectOuRefId).build();
        Stream<DBOrgUnit> allProjectChildSet=Stream.of(cloudOu,modernOu,mobileOu);
        ouHelper.insertNewOrgUnits(company, allProjectChildSet);
        DbListResponse<DBOrgUnit> projectOuList=ouService.getOusForGroupId(company,projectCatId);
        projectOuList.getRecords().stream().forEach(unit->{
                    if(unit.getParentRefId()!=null){
                        try {
                            configService.insert(company, TenantConfig.builder()
                                    .name(projectsCategory.getName() + DOUBLE_HASH + allprojectsOU.getName() + DOUBLE_HASH + unit.getName())
                                    .value(String.valueOf(unit.getRefId()))
                                    .build());
                        }catch(SQLException e){
                            log.error("[{}] Error occur while inserting into tenant config: {}", company, e);
                        }
                    }
                }
        );

        return productId;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    // no deletion allowed for the time being
    // @RequestMapping(method = RequestMethod.DELETE, value = "/{productid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> productDelete(@PathVariable("productid") String productId,
                                                                        @SessionAttribute(name = "session_user") String sessionUser,
                                                                        @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            var workitems = workItemsClient.list(company, DefaultListRequest.builder().page(0).pageSize(1).filter(Map.of("product_ids", List.of(productId))).build());
            if (workitems.getMetadata().getTotalCount() > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete workspace. The selected workspace(s) cannot be deleted as there are " + workitems.getMetadata().getTotalCount() + " CI/CD Jobs associated with the workspace(s).");
            }
            DbListResponse<CICDJob> dbListResponse = ciCdJobsDatabaseService.listByFilter(company, 0, 10, null, null, null, null, null);
            if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete workspace. The selected workspace(s) cannot be deleted as there are " + workitems.getMetadata().getTotalCount() + " CI/CD Jobs associated with the workspace(s).");
            }

            Optional<Product> dbProduct = productService.get(company, productId);
            if (dbProduct.filter(p -> BooleanUtils.isTrue(p.getImmutable())).isPresent()) {
                throw new BadRequestException("Cannot remove read-only project.");
            }
            try {
                productService.delete(company, productId);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(productId)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", productId))
                        .details(Collections.emptyMap())
                        .action(ActivityLog.Action.DELETED)
                        .build());
            } catch (Exception e) {
                String msg = e.getMessage();
                Matcher matcher = FOREIGN_KEY_VIOLATION.matcher(msg);
                if (matcher.find()) {
                    String tableName = matcher.group("tblname");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove products when there is associated data. This product is used in " + tableName);
                }
                return ResponseEntity.ok(DeleteResponse.builder().id(productId).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(productId).success(true).build());
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteProducts(@RequestBody List<String> productIds,
                                                                             @SessionAttribute(name = "session_user") String sessionUser,
                                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            var workitems = workItemsClient.list(company, DefaultListRequest.builder().page(0).pageSize(1).filter(Map.of("product_ids", productIds)).build());
            if (workitems.getMetadata().getTotalCount() > 0) {
                List<String> ids = ListUtils.emptyIfNull(workitems.getResponse().getRecords()).stream().map(WorkItem::getProductId).distinct().collect(Collectors.toList());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove products when there are tickets associated with it. There are " + workitems.getMetadata().getTotalCount() + " tickets associated to the products: " + ids);
            }
            DbListResponse<CICDJob> dbListResponse = ciCdJobsDatabaseService.listByFilter(company, 0, 10, null, null, null, null, null);
            if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove products when there are CiCD Jobs associated with it. There are " + dbListResponse.getTotalCount() + " CiCd Jobs associated to the products");
            }
            List<String> filteredIds = productIds.stream()
                    .map(NumberUtils::toInt)
                    .map(Number::toString)
                    .collect(Collectors.toList());
            try {
                productService.bulkDelete(company, filteredIds);
                productIds.forEach(productId -> {
                    try {
                        activityLogService.insert(company, ActivityLog.builder()
                                .targetItem(productId)
                                .email(sessionUser)
                                .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                                .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", productId))
                                .details(Collections.emptyMap())
                                .action(ActivityLog.Action.DELETED)
                                .build());
                    } catch (SQLException e) {
                        log.error("Unable to insert the log in activity log for product with id: " + productId + ". " + e.getMessage());
                    }
                });
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(filteredIds, true, null));
            } catch (Exception e) {
                String msg = e.getMessage();
                Matcher matcher = FOREIGN_KEY_VIOLATION.matcher(msg);
                if (matcher.find()) {
                    String tableName = matcher.group("tblname");
                    return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(filteredIds, false, "Cannot remove products when there is associated data. This product is used in" + tableName));
                }
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(filteredIds, false, e.getMessage()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{productid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> productUpdate(@RequestBody Product product,
                                                              @SessionAttribute(name = "session_user") String sessionUser,
                                                              @PathVariable("productid") String productId,
                                                              @SessionAttribute(name = "company") String company) {
        if (StringUtils.isEmpty(product.getId())) {
            product = Product.builder()
                    .id(productId)
                    .name(product.getName())
                    .description(product.getDescription())
                    .ownerId(product.getOwnerId())
                    .integrationIds(product.getIntegrationIds())
                    .build();
        }
        final Product finalProduct = product;
        return SpringUtils.deferResponse(() -> {
            validateInput(finalProduct, false);
            var productExist = productService.get(company, productId).get();
            if (!Objects.isNull(finalProduct.getDisabled()) && finalProduct.getDisabled() != productExist.getDisabled()) {
                log.info("Going here woot woot noo");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Updating the disabled field is not allowed through this endpoint. Please use the /disabled endpoint instead.");
            }
            Set<Integer> productList = productExist.getIntegrationIds();
            if (!productList.isEmpty() && finalProduct.getIntegrationIds() != null) {
                productList.removeAll(finalProduct.getIntegrationIds());
            }
            try {
                productService.update(company, finalProduct);
            } catch (Exception e) {
                String msg = e.getMessage();
                Matcher matcher = DUPLICATE_KEY_VALUE_ERROR.matcher(msg);
                if (matcher.find()) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Key already exists");
                }
                Matcher nameMatcher = DUPLICATE_NAME_VALUE_ERROR.matcher(msg);
                if (nameMatcher.find()) {
                    log.error(e.getMessage());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Name already exists");
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!productList.isEmpty() && finalProduct.getIntegrationIds() != null && !finalProduct.getIntegrationIds().isEmpty()) {
                productService.deleteOuIntegration(company, Integer.parseInt(finalProduct.getId()), productList.stream().collect(Collectors.toList()));
            }
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(productId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited", productId))
                    .details(Collections.singletonMap("item", finalProduct))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            // check for OUs with matching configurations for the integrations no longer in the mix
            var filters = QueryFilter.builder()
                    .strictMatch("ou_category_id", Set.of())
                    .build();
            // ouService.filter(company, filters, 0, 50);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{productid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Product>> productDetails(@PathVariable("productid") String productId,
                                                                  @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(productService.get(company, productId)
                .orElseThrow(() -> new NotFoundException("Product with id " + productId + " not found."))));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','ASSIGNED_ISSUES_USER', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Product>>> productsList(@SessionAttribute(name = "company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String partialName = null;
            Set<Integer> productIds = null;
            Boolean bootstrapped = null;
            Boolean immutable = null;
            Boolean demo = null;
            if (filter.getFilter() != null && filter.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()) != null) {
                // this partial name is looked up in the middle: %name%
                // TODO: we are supporting {"partial":{"name":{"starts":"value"}}} so we can look it up like 'value%' which is faster and more common
                // so, we need to support both.. string value and map
                var a = ((Map<String, Object>) filter.getFilter()
                        .getOrDefault("partial", Collections.emptyMap()))
                        .getOrDefault("name", null);
                if (a != null && a instanceof String) {
                    partialName = (String) a;
                } else if (a != null && a instanceof Map) {
                    var b = (Map<String, String>) a;
                    partialName = b.get("starts");
                }
                productIds = ((List<String>) filter.getFilter()
                        .getOrDefault("product_ids", Collections.emptyList())).stream().map(i -> Integer.parseInt(i)).collect(Collectors.toSet());
                bootstrapped = filter.getFilterValue("bootstrapped", Boolean.class).orElse(null);
                immutable = filter.getFilterValue("immutable", Boolean.class).orElse(null);
                demo=filter.getFilterValue("demo", Boolean.class).orElse(null);
            }

            Map<String, Integer> updateRange = filter.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;

            Set<Integer> integrationIds = filter.<Integer>getFilterValueAsSet("integration_id").orElse(Set.of());
            Set<String> integrationType = filter.<String>getFilterValueAsSet("integration_type").orElse(Set.of());
            Set<String> category = filter.<String>getFilterValueAsSet("category").orElse(Set.of());
            Set<String> key = filter.<String>getFilterValueAsSet("key").orElse(Set.of());
            Set<String> ownerId = filter.<String>getFilterValueAsSet("owner_id").orElse(Set.of());
            Set<String> orgIdentifier = filter.<String>getFilterValueAsSet("orgIdentifier").orElse(Set.of());
            DbListResponse<Product> products = productService.listByFilter(
                    company,
                    partialName,
                    productIds,
                    integrationIds,
                    integrationType,
                    category,
                    key,
                    orgIdentifier,
                    ownerId,
                    bootstrapped, immutable, updatedAtStart, updatedAtEnd, null, demo, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), products));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/{productid:[0-9]+}/integrations/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Integration>>> listIntegrationsByProduct(
            @PathVariable("productid") Integer productId, @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String partialName = (String) filter.<String, Object>getFilterValueAsMap("partial").orElse(Map.of()).
                    getOrDefault("name", null);
            List<Integer> tagIds = filter.<Integer>getFilterValueAsList("tag_ids").orElse(List.of());
            List<String> applications = filter.<String>getFilterValueAsList("applications").orElse(List.of());
            Boolean satellite = filter.getFilterValue("satellite", Boolean.class).orElse(null);
            DbListResponse<Integration> integrations = productService.listIntegrationsByFilter(company, partialName,
                    false, applications, satellite, tagIds, productId, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), integrations));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/{id}/categories/dashboard/{dashboard_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgUnitCategory>>> getCategories(@SessionAttribute(name = "company") final String company,
                                                                                            @RequestBody DefaultListRequest request, @PathVariable("id") Integer workspaceId, @PathVariable("dashboard_id") Integer dashboardId) {
        return SpringUtils.deferResponse(() -> {
            var filters = QueryFilter.fromRequestFilters(request.getFilter());
            var listResponse = categoryService.filterByDashboard(company, workspaceId, dashboardId, filters, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    listResponse.getTotalCount(),
                    listResponse.getRecords()));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{productid:[0-9]+}/disable", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> productDisable(@RequestParam Boolean disabled,
                                                                         @PathVariable("productid") String productId,
                                                                         @SessionAttribute(name = "session_user") String sessionUser,
                                                                         @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            Product dbProduct = productService.get(company, productId).get();
            if (dbProduct.getImmutable()) {
                throw new BadRequestException("Cannot disable read-only project.");
            }
            try {
                Product updatedProduct = dbProduct.toBuilder().disabled(disabled).build();
                productService.update(company, updatedProduct);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(productId)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Disabled", productId))
                        .details(Collections.emptyMap())
                        .action(ActivityLog.Action.EDITED)
                        .build());
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(productId).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(productId).success(true).build());
        });
    }

    @GetMapping(value = "/orgIdentifier/{orgIdentifier}/projectIdentifier/{projectIdentifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> getWorkspaceFromHarnessProject(@SessionAttribute(name = "company") final String company,
                                                              @PathVariable String orgIdentifier, @PathVariable String projectIdentifier){

            var product = productService.getProduct(company, orgIdentifier, projectIdentifier).orElse(null);
            if(product == null){
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            return  ResponseEntity.ok(product);

    }
}
