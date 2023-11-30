import {
  ApiKeysList,
  Configuration,
  GlobalSettings,
  PluginResultContainer,
  PluginResultsList,
  SignatureDetails,
  SignaturesList,
  SLASettings,
  SsoSettings,
  ToolDetails,
  UserEdit,
  UsersList,
  ViolationLogsList,
  VelocityConfigsListPage,
  TrellisProfile,
  TrellisScoreProfile
} from "../configurations/pages";
import { ProfileEdit } from "../profile";
import { AddIntegrationLandingPage } from "../configurations/pages/integrations";
import IntegrationsLandingPage from "../configurations/pages/integrations/integrations-landing-page";
import { IntegrationEdit } from "../configurations/containers/integration-steps";
import { IntegrationAddPageNew } from "../configurations/pages/integrations/gitlab-integration/integration-add.page";
import {
  AUDIT_LOGS_ROUTES,
  TRELLIS_SCORE_PROFILE_ROUTES,
  getIntegrationPage,
  Organization_Routes,
  ORGANIZATION_USERS_ROUTES,
  SELF_ONBOARDING_ROUTES,
  TICKET_CATEGORIZATION_SCHEMES_ROUTES,
  getSettingsPage,
  getBaseUrl,
  getReportsPage,
  TRELLIS_SCORE_CENTRAL_PROFILE_ROUTES
} from "../constants/routePaths";
import TicketCategorizationProfilesLandingPage from "configurations/pages/ticket-categorization/containers/ticket-categorization-landing-container/TicketCategorizationProfile";
import ProfileContainer from "configurations/pages/ticket-categorization/containers/profile-container/ProfileContainer";
import CategoriesContainer from "configurations/pages/ticket-categorization/containers/categories-container/CategoriesContainer";
import { USERROLES, USER_ADMIN_ROLES } from "./helper/constants";
import OrganizationUnitCreateContainer from "configurations/pages/Organization/organization-unit/container/OrganizationUnitCreateContainer";
import OrganizationUnitEditContainer from "configurations/pages/Organization/organization-unit/container/OrgUnitEditContainer";
import AuditLogs from "configurations/pages/auditLogs/auditLogs.list";
import IntegrationConnectContainer from "configurations/pages/self-onboarding/containers/integration-connect-container/IntegrationConnectContainer";
import IntegrationEditConnectContainer from "configurations/pages/self-onboarding/containers/integration-edit-connect-container/IntegrationEditConnectContainer";
import { BASE_UI_URL } from "helper/envPath.helper";
import EffortInvestmentHeader from "./customHeaders/effort-investment-header";
import WorkflowProfilesHeader from "./customHeaders/workflow-profiles-header";
import TrellisScoreProfilesHeader from "./customHeaders/trellis-score-profiles-header";
import OrganizationUsersListContainer from "configurations/pages/Organization/Container/OrganizationUsersListContainer";
import WorkflowProfileDetailsContainer from "configurations/pages/lead-time-profiles/containers/WorkflowProfileDetailsContainer";
import IntegrationMappings from "configurations/pages/IntegrationMappings/IntegrationMappings";
import { projectPathPropsDef } from "utils/routeUtils";
import TrellisCentralProfilePage from "configurations/pages/TrellisProfile/LandingPage/TrellisCentralProfilePage";
import OrganizationListPage from "configurations/pages/Organization/Container/OrganizationListPage";
import withHarnessPermission from "hoc/withHarnessPermission";

export function configurationRoutes({ hideAppSignatures, hideAppViolation, hideAppTools }) {
  return [
    {
      name: "Configurations",
      path: "/configuration",
      layout: getBaseUrl(),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      component: withHarnessPermission(Configuration),
      collapse: false,
      id: "configuration",
      label: "Settings",
      fullPath: getSettingsPage(),
      icon: "settings",
      actions: {},
      items: [
        {
          id: "configuration",
          label: "Settings",
          path: "",
          icon: "",
          description: "Configure your environment"
        },
        // {
        //   id: "plugin-result-detail",
        //   label: "Plugin Result",
        //   path: "/plugin-results-details",
        //   icon: "",
        //   description: "View detailed plugin result"
        // },
        {
          // Being Used from integrations page...
          id: "plugin-result",
          label: "Plugin Results",
          path: "/plugin-results",
          icon: "",
          description: "View Plugin Results, diff and print reports"
        },
        {
          id: "integrations",
          path: "/integrations",
          rbac: USER_ADMIN_ROLES,
          icon: "integrations",
          description: "Manage integrations",
          label: "Integrations",
          hasAction: false
        },
        {
          id: "editusers",
          path: "/edit-user-page",
          rbac: USER_ADMIN_ROLES,
          description: "Create and edit users",
          label: "Contributors",
          hasAction: false,
          dynamicHeader: true,
          actionId: "editusers"
        },
        {
          id: "view-question",
          path: "/view-questions-page",
          rbac: USER_ADMIN_ROLES,
          description: "View and manage questions",
          label: "Question Bank",
          hasAction: true,
          actionId: "add-question",
          actionRoute: "add-question-page",
          actionLabel: "Add Section",
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getSettingsPage()}/add-question-page?tag=0`));
          }
        },
        {
          hasAction: false,
          id: "users",
          rbac: USER_ADMIN_ROLES,
          path: "/users-page",
          actionLabel: "Add User",
          dynamicHeader: true,
          actionId: "add-user",
          actionRoute: "add-user-page",
          label: "Accounts",
          description: "View and manage users",
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getSettingsPage()}/add-user-page`));
          }
        },
        {
          hasAction: false,
          id: "sso-page",
          rbac: USER_ADMIN_ROLES,
          path: "/sso-page",
          label: "SSO Settings",
          description: "Manage SSO settings"
        },
        {
          hasAction: false,
          id: "apikeys",
          rbac: USER_ADMIN_ROLES,
          path: "/apikeys",
          actionLabel: "Add Apikey",
          actionId: "add-apikey",
          actionRoute: "add-apikey",
          label: "Apikeys",
          dynamicHeader: true,
          description: "View and manage api keys",
          buttonHandler: () => {
            console.log("api keys");
          }
        },
        {
          hasAction: false,
          id: "global-page",
          rbac: USER_ADMIN_ROLES,
          path: "/global",
          actionLabel: "Add State",
          actionId: "add-state",
          actionRoute: "add-state",
          label: "Customize",
          dynamicHeader: true,
          description: "Customize",
          buttonHandler: () => {
            console.log("Customize");
          }
        },
        {
          path: "/sla",
          id: "sla",
          label: "SLA",
          layout: getBaseUrl(),
          name: "SLA",
          //component: SLASettings,
          rbac: USER_ADMIN_ROLES,
          icon: "signatures"
        },
        {
          id: "add-user-page",
          path: "/add-user-page",
          rbac: USER_ADMIN_ROLES,
          label: "Add User Page",
          layout: getBaseUrl(),
          dynamicHeader: true
        },
        {
          id: "edit-integration",
          path: "/integrations/edit",
          label: "Edit Integration",
          layout: getBaseUrl(),
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          id: "new-integration-types",
          path: "/integrations/types",
          label: "Integrations",
          rbac: USER_ADMIN_ROLES,
          hasAction: true,
          actionLabel: "Cancel",
          actionButtonType: "secondary",
          dynamicHeader: true,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getSettingsPage()}/integrations`));
          }
        },
        {
          id: "new-add-integration-page",
          path: "/integrations/new-add-integration-page",
          label: "Integrations",
          rbac: USER_ADMIN_ROLES,
          dynamicHeader: true
        },
        {
          id: "effort-investment",
          path: "/effort-investment",
          label: "Effort Investment Profiles",
          description:
            "Profiles help you track and manage engineering effort investment towards business initiatives, strategic areas, task types and more.",
          showDescription: true,
          layout: getBaseUrl(),
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          renderCustomHeader: EffortInvestmentHeader
        },
        {
          path: "/effort-investment/profiles/:id",
          layout: getBaseUrl(),
          name: "Add/Edit Effort Investment Profile",
          mini: "TCS",
          showLabel: false,
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          id: "effort-investment-profile-add-edit",
          description: "",
          showDescription: true,
          dynamicHeader: true
        },
        {
          path: "/effort-investment/profiles/:id/categories/:categoryId",
          layout: getBaseUrl(),
          name: "Add/Edit Effort Investment Profile Categories",
          mini: "TCS",
          showLabel: false,
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          id: "effort-investment-profile-add-edit-categories",
          dynamicHeader: true
        },
        {
          id: "lead-time-profile",
          path: "/lead-time-profile",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          label: "Workflow Profiles",
          layout: getBaseUrl(),
          description:
            "Create profiles that defines the stages, events, and measuring criteria in your Lead Time report.",
          showDescription: true,
          renderCustomHeader: WorkflowProfilesHeader
        },
        {
          id: "lead-time-profile-edit",
          path: "/lead-time-profile/edit",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          label: "Workflow Profile",
          layout: getBaseUrl(),
          description: "",
          showDescription: true,
          dynamicHeader: true
        },
        {
          id: "organization-users",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          path: "/organization_users",
          label: "Organization Contributors",
          description: "View and manage collections users",
          dynamicHeader: true
        },
        {
          hasAction: true,
          id: "organization",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          path: "/organization",
          layout: getBaseUrl(projectPathPropsDef),
          label: "Collections",
          description: "View and manage collections",
          dynamicHeader: true
        },
        {
          id: "org-unit-create",
          path: "/organization/create_org_unit",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          label: "Create Collection",
          layout: getBaseUrl(projectPathPropsDef),
          description: "Create Collection",
          dynamicHeader: true
        },
        {
          id: "org-unit-edit",
          path: "/organization/create_org_unit/:id",
          rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
          label: "Edit Collection",
          layout: getBaseUrl(projectPathPropsDef),
          description: "Edit Collection",
          dynamicHeader: true
        },
        {
          id: "trellis_score",
          path: "/trellis_score_profile",
          rbac: ["admin"],
          label: "Trellis Score Profile",
          layout: getBaseUrl(),
          description:
            "Trellis score is a proprietary scoring mechanism from SEI that helps you to understand your team's productivity. Trellis score uses many factors such as code quality, code volume, speed, impact, proficiency, and collaboration.",
          showDescription: true,
          renderCustomHeader: TrellisScoreProfilesHeader
        },
        {
          id: "trellis_score_edit",
          path: "/trellis_score_profile/profile/:id",
          rbac: ["admin"],
          showLabel: false,
          layout: getBaseUrl(),
          showDescription: true,
          dynamicHeader: true
        },
        {
          id: "self-onboarding-connect",
          path: "/add-integration/:id",
          rbac: USER_ADMIN_ROLES,
          label: "Self Onboarding",
          layout: getBaseUrl(),
          description: "",
          dynamicHeader: true
        },
        {
          id: "self-onboarding-integration-edit",
          path: "/edit-integration/:id",
          rbac: USER_ADMIN_ROLES,
          label: "Self Onboarding",
          layout: getBaseUrl(),
          description: "",
          dynamicHeader: true
        },
        {
          id: "trellis_central_profile",
          path: "/trellis_central_profile",
          rbac: [...USER_ADMIN_ROLES],
          label: "Trellis Central Profile",
          description: "",
          dynamicHeader: true
        }
      ]
    },
    {
      path: "/configuration/sla",
      layout: getBaseUrl(),
      name: "SLA",
      label: "SLA",
      id: "sla",
      icon: "slaNew",
      component: withHarnessPermission(SLASettings),
      rbac: USER_ADMIN_ROLES,
      settingsDescription: "Configure SLAs and use them as filters on insights.",
      settingsGroupId: 2
    },
    {
      path: "/configuration/view-signatures",
      layout: getBaseUrl(),
      name: "Signatures",
      mini: "SiIG",
      hide: hideAppSignatures,
      component: withHarnessPermission(SignaturesList),
      rbac: USER_ADMIN_ROLES,
      id: "signatures",
      fullPath: `${getSettingsPage()}/view-signatures`,
      description: "",
      label: "Signatures",
      icon: "signatures"
    },
    {
      path: "/configuration/view-violation-logs",
      layout: getBaseUrl(),
      name: "Violation Logs",
      mini: "VLOGS",
      hide: hideAppViolation,
      component: withHarnessPermission(ViolationLogsList),
      rbac: USER_ADMIN_ROLES,
      id: "violation-logs",
      fullPath: `${getSettingsPage()}/view-violation-logs`,
      description: "",
      label: "Violation Logs",
      icon: "violationLogs"
    },
    {
      path: "/configuration/signature-detail-page",
      layout: getBaseUrl(),
      name: "Signatures",
      mini: "SiIG",
      hide: hideAppSignatures,
      component: withHarnessPermission(SignatureDetails),
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/plugin-results",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      hide: hideAppTools,
      component: withHarnessPermission(PluginResultsList),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/plugin-results-details",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      hide: hideAppTools,
      component: withHarnessPermission(PluginResultContainer),
      invisible: true,
      rbac: ["admin", "auditor"]
    },
    {
      path: "/configuration/edit-tools-page",
      layout: getBaseUrl(),
      name: "List Tools",
      mini: "TT",
      hide: hideAppTools,
      component: withHarnessPermission(ToolDetails),
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/sso-page",
      layout: getBaseUrl(),
      name: "SSO Settings",
      mini: "SSO",
      component: withHarnessPermission(SsoSettings),
      rbac: USER_ADMIN_ROLES,
      id: "sso-page",
      fullPath: `${getSettingsPage()}/sso-page`,
      label: "SSO Settings",
      description: "Configure SSO access",
      icon: "ssoNew",
      settingsDescription: "Configure Single Sign On.",
      settingsGroupId: 1
    },
    {
      path: "/configuration/global",
      layout: getBaseUrl(),
      name: "Customize",
      mini: "Global",
      component: withHarnessPermission(GlobalSettings),
      rbac: USER_ADMIN_ROLES,
      id: "global-page",
      fullPath: `${getSettingsPage()}/global`,
      label: "Customize",
      description: "Customize",
      icon: "globalSettingsNew",
      settingsDescription:
        "Customize settings for Authentication, User Roles, SCM, Custom Colors, States and Chrome Whitelisted URLs.",
      settingsGroupId: 1
    },
    {
      path: "/configuration/apikeys",
      layout: getBaseUrl(),
      name: "Apikeys",
      mini: "AK",
      component: withHarnessPermission(ApiKeysList),
      rbac: USER_ADMIN_ROLES,
      id: "apikeys",
      fullPath: `${getSettingsPage()}/apikeys`,
      label: "API Keys",
      description: "Manage API Keys",
      icon: "apiKeyNew",
      settingsDescription: "Create and maange API keys.",
      settingsGroupId: 2
    },
    {
      path: "/settings/new-sso-page",
      layout: getBaseUrl(),
      name: "SSO Settings",
      mini: "SSO",
      component: withHarnessPermission(SsoSettings),
      rbac: USER_ADMIN_ROLES,
      id: "sso-page-settings",
      fullPath: `${getReportsPage()}/settings/new-sso-page`,
      label: "SSO Settings",
      description: "Configure SSO access",
      icon: "sso"
    },
    {
      path: "/configuration/users-page",
      layout: getBaseUrl(),
      name: "Contributors",
      mini: "UP",
      component: UsersList,
      rbac: USER_ADMIN_ROLES,
      id: "user-page",
      fullPath: `${getSettingsPage()}/users-page`,
      label: "Accounts",
      description: "Account Contributors",
      icon: "propeloAccountsNew",
      settingsDescription: "Create and manage accounts including Admins, Standard and Limited accounts.",
      settingsGroupId: 1
    },
    {
      path: "/configuration/add-user-page",
      layout: getBaseUrl(),
      name: "New User",
      mini: "NU",
      component: UserEdit,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/edit-user-page",
      layout: getBaseUrl(),
      name: "Edit User",
      mini: "EU",
      component: UserEdit,
      invisible: true,
      rbac: USER_ADMIN_ROLES
    },
    {
      path: "/configuration/user-page",
      layout: getBaseUrl(),
      name: "User Page",
      mini: "UP",
      component: ProfileEdit,
      invisible: true,
      rbac: Object.values(USERROLES)
    },
    {
      path: "/configuration/integrations",
      layout: getBaseUrl(),
      name: "Integrations",
      mini: "IN",
      component: withHarnessPermission(IntegrationsLandingPage),
      rbac: USER_ADMIN_ROLES,
      id: "integrations",
      fullPath: `${getSettingsPage()}/integrations`,
      description: "Manage integrations",
      label: "Integrations",
      icon: "integrationsNew",
      settingsDescription:
        "Add or manage integrations such as such as JIRA, GitHub, GitLab, Jenkins, PagerDuty or many others.",
      settingsGroupId: 2
    },
    {
      path: "/configuration/integrations/edit",
      layout: getBaseUrl(),
      name: "Edit Integrations",
      mini: "IN",
      component: withHarnessPermission(IntegrationEdit),
      rbac: USER_ADMIN_ROLES,
      id: "edit-integrations",
      fullPath: `${getSettingsPage()}/integrations/edit`,
      description: "Edit integrations",
      label: "Edit Integrations",
      icon: "integrations"
    },
    {
      path: "/configuration/integrations/types",
      layout: getBaseUrl(),
      name: "Edit Integrations",
      mini: "IN",
      component: withHarnessPermission(AddIntegrationLandingPage),
      rbac: USER_ADMIN_ROLES,
      id: "new-integrations-types",
      fullPath: `${getIntegrationPage()}/types`,
      description: "Edit Integrations",
      label: "Edit Integrations",
      icon: "integrations"
    },
    {
      path: "/configuration/integrations/new-add-integration-page",
      layout: getBaseUrl(),
      name: "Add/Edit Integrations",
      mini: "IN",
      component: withHarnessPermission(IntegrationAddPageNew),
      rbac: USER_ADMIN_ROLES,
      id: "new-add-integrations-page",
      fullPath: `${getIntegrationPage()}/new-add-integration-page`,
      description: "Add Integrations",
      label: "Add Integrations",
      icon: "integrations"
    },
    {
      path: "/configuration/effort-investment",
      layout: getBaseUrl(),
      name: "Add/Edit Effort Investment Profiles",
      mini: "TCS",
      component: withHarnessPermission(TicketCategorizationProfilesLandingPage),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "effort-investment",
      fullPath: TICKET_CATEGORIZATION_SCHEMES_ROUTES._ROOT,
      description: "Add Effort Investment Profiles",
      label: "Investment Profiles",
      icon: "alignProfileNew",
      settingsDescription:
        "Effort Investment profiles help you align planned and completed work with company initiatives.",
      settingsGroupId: 3
    },
    {
      path: `${TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE}/:id`,
      layout: getBaseUrl(),
      name: "Add/Edit Effort Investment Profiles",
      mini: "TCS",
      component: withHarnessPermission(ProfileContainer),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "effort-investment-profile-add-edit"
    },
    {
      path: `${TICKET_CATEGORIZATION_SCHEMES_ROUTES.EDIT_CREATE}/:id/categories/:categoryId`,
      layout: getBaseUrl(),
      name: "Add/Edit Effort Investment Profile Categories",
      mini: "TCS",
      component: withHarnessPermission(CategoriesContainer),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "effort-investment-profile-add-edit-categories"
    },
    {
      path: "/configuration/lead-time-profile",
      layout: getBaseUrl(),
      name: "Workflow Profiles",
      mini: "LTP",
      component: withHarnessPermission(VelocityConfigsListPage),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "lead-time-profile",
      fullPath: `${getSettingsPage()}/lead-time-profile`,
      label: "Workflow Profiles",
      description: "Manage Workflow Profiles",
      icon: "workflowProfileNew",
      settingsDescription: "Create profiles that define stages, events and thresholds for improving your workflow.",
      settingsGroupId: 3
    },
    {
      path: "/configuration/lead-time-profile/edit",
      layout: getBaseUrl(),
      name: "Workflow Profile",
      mini: "LTP",
      component: withHarnessPermission(WorkflowProfileDetailsContainer),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "lead-time-profile-edit"
    },
    {
      path: Organization_Routes._ROOT,
      layout: getBaseUrl(projectPathPropsDef),
      name: "Collections",
      mini: "OG",
      component: OrganizationListPage,
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "organization",
      fullPath: `${getBaseUrl(projectPathPropsDef)}${Organization_Routes._ROOT}`,
      label: "Collections",
      description: "View and manage collections",
      icon: "cluster",
      settingsDescription:
        "Create and manage Collections. A Collection is a flexible way of defining teams and can be used to filter data in the insights.",
      settingsGroupId: 2
    },
    {
      path: ORGANIZATION_USERS_ROUTES._ROOT,
      layout: getBaseUrl(),
      name: "Contributors",
      mini: "OGU",
      component: withHarnessPermission(OrganizationUsersListContainer),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "organization-users",
      fullPath: `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`,
      label: "Contributors",
      icon: "orgNew",
      settingsDescription: "Add and manage contributors (SCM and other application contributors) and contributor attributes.",
      settingsGroupId: 2
    },
    {
      path: Organization_Routes.CREATE_ORG_UNIT,
      layout: getBaseUrl(projectPathPropsDef),
      name: "Create Collection",
      mini: "OG",
      component: OrganizationUnitCreateContainer,
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "create_org_unit",
      fullPath: `${getBaseUrl(projectPathPropsDef)}${Organization_Routes.CREATE_ORG_UNIT}`,
      label: "Create Collection",
      icon: "group"
    },
    {
      path: Organization_Routes.EDIT_ORG_UNIT,
      layout: getBaseUrl(projectPathPropsDef),
      name: "EDIT Collection",
      mini: "OG",
      component: OrganizationUnitEditContainer,
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "edit_org_unit",
      fullPath: `${getBaseUrl(projectPathPropsDef)}${Organization_Routes.EDIT_ORG_UNIT}`,
      label: "Edit Collection",
      icon: "group"
    },
    {
      path: TRELLIS_SCORE_PROFILE_ROUTES._ROOT,
      layout: getBaseUrl(),
      name: "Trellis Score Profiles",
      mini: "TSP",
      component: withHarnessPermission(TrellisScoreProfile),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "trellis_score_profile",
      label: "Trellis Score Profiles",
      description: "View and manage dev productivity profile",
      icon: "devProfileNew",
      settingsDescription:
        "Trellis score Profiles allow you to customize and configure factors that make up the Trellis score.",
      settingsGroupId: 3
    },
    {
      path: TRELLIS_SCORE_CENTRAL_PROFILE_ROUTES._ROOT,
      layout: getBaseUrl(),
      name: "Trellis Factors",
      mini: "TSP",
      component: withHarnessPermission(TrellisCentralProfilePage),
      rbac: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
      id: "trellis_central_profile",
      label: "Trellis Factors",
      description: "View and manage dev productivity profile",
      icon: "devProfileNew",
      settingsDescription:
        "Trellis template allow you to customize standards and thresholds for propelling developers in your organization.",
      settingsGroupId: 3,
      fullPath: `${getBaseUrl()}${TRELLIS_SCORE_CENTRAL_PROFILE_ROUTES._ROOT}`
    },
    {
      path: `${TRELLIS_SCORE_PROFILE_ROUTES.EDIT}/:id`,
      layout: getBaseUrl(),
      name: "Trellis Score Profile",
      mini: "TSP",
      showLabel: true,
      label: "Trellis Score Profiles",
      component: withHarnessPermission(TrellisProfile),
      rbac: ["admin", USERROLES.ORG_UNIT_ADMIN],
      id: "edit_trellis_score_profile",
      description: "Edit Trellis profile"
    },
    {
      path: AUDIT_LOGS_ROUTES._ROOT,
      layout: getBaseUrl(),
      name: "Activity Logs",
      mini: "auditlogs",
      component: withHarnessPermission(AuditLogs),
      rbac: USER_ADMIN_ROLES,
      id: "audit_logs",
      fullPath: `${getBaseUrl()}${AUDIT_LOGS_ROUTES._ROOT}`,
      label: "Activity Logs",
      description: "View Activity Logs",
      icon: "auditLogsNew",
      settingsDescription: "View contributor activity across the tenant.",
      settingsGroupId: 1
    },
    {
      path: `${SELF_ONBOARDING_ROUTES._ROOT}/:id`,
      layout: getBaseUrl(),
      name: "Self Onboarding",
      mini: "ITP",
      component: withHarnessPermission(IntegrationConnectContainer),
      rbac: USER_ADMIN_ROLES,
      id: "self-onboarding-connect",
      label: "Self Onboarding",
      description: "",
      icon: "integrations"
    },
    {
      path: `${SELF_ONBOARDING_ROUTES._EDIT}/:id`,
      layout: getBaseUrl(),
      name: "Self Onboarding",
      mini: "ITP",
      component: withHarnessPermission(IntegrationEditConnectContainer),
      rbac: USER_ADMIN_ROLES,
      id: "self-onboarding-integration-edit",
      label: "Self Onboarding",
      description: "",
      icon: "integrations"
    },
    {
      path: "/sei-integration-mapping",
      layout: getBaseUrl(projectPathPropsDef),
      name: "Integration Mappings",
      component: IntegrationMappings,
      rbac: USER_ADMIN_ROLES,
      id: "integration-mapping",
      label: "Integration Mapping",
      description: "",
      icon: "integrations"
    }
  ];
}
