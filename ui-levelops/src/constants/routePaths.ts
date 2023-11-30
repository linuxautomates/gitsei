import { ProjectPathProps } from "classes/routeInterface";

export const getBaseUrl = (props?: ProjectPathProps): string => {
  return window.baseUrl || "/admin";
};

export const getBaseUrlWhenNoScopeSelected = (props: ProjectPathProps) => {
  return `${window.baseUrl}/orgs/${props.orgIdentifier}/projects/${props.projectIdentifier}`;
};

// Sign in paths
export const LOGIN_PAGE = "/auth/login-page";
export const SIGN_IN_PAGE = "/signin";

export const WORKSPACE_PATH = "/workspaces";
export const WORKSPACE_ADD_PATH = `${WORKSPACE_PATH}/add-workspace-page`;
export const WORKSPACE_EDIT_PATH = `${WORKSPACE_PATH}/edit-workspace-page`;
export const HOME_PATH = "/sei-home";

export const DASHBOARD_ROUTES = {
  _ROOT: "/dashboards",
  SECURITY: "/dashboard",

  LIST: "/dashboards",
  CREATE: "/dashboards/create",
  DRILL_DOWN: "/dashboards/drill-down"
};

export const CONFIG_TABLE_ROUTES = {
  _ROOT: "/tables",

  LIST: "/tables",
  CREATE: "/tables/create",
  EDIT: "/tables/edit"
};

export const TEMPLATE_ROUTES = {
  _ROOT: "/templates",

  ASSESSMENT_TEMPLATES: {
    LIST: "/templates/assessment-templates",
    CREATE: "/templates/assessment-templates/create",
    EDIT: "/templates/assessment-templates/edit"
  },

  ISSUE_TEMPLATE: {
    LIST: "/templates/issue-templates",
    EDIT: "/templates/issue-templates/edit"
  },

  KB: {
    LIST: "/templates/knowledge-bases",
    CREATE: "/templates/knowledge-bases/create",
    EDIT: "/templates/knowledge-bases/edit"
  },

  COMMUNICATION_TEMPLATES: {
    LIST: "/templates/communication-templates",
    CREATE: "/templates/communication-templates/create",
    EDIT: "/templates/communication-templates/edit"
  }
};

export const TRIAGE_ROUTES = {
  _ROOT: "/triage",
  DETAIL: "/triage/view",
  LIST: "/triage/rules",
  CREATE: "/triage/rules/create",
  EDIT: "/triage/rules/edit",
  JOB_STAGES: "/triage/stages",
  RESULTS: "/triage/results"
};

export const PROPELS_ROUTES = {
  _ROOT: "/propels",
  RUNS_LOGS: "/propels/runs-logs",
  PROPEL_EDITOR: "/propels/propels-editor",
  AUTOMATION_RULES: "/propels/automation-rules"
};

export const TICKET_CATEGORIZATION_SCHEMES_ROUTES = {
  _ROOT: "/configuration/effort-investment",
  EDIT_CREATE: "/configuration/effort-investment/profiles"
};

export const SELF_ONBOARDING_ROUTES = {
  _ROOT: "/configuration/add-integration",
  _EDIT: "/configuration/edit-integration"
};

export const VELOCITY_CONFIGS_ROUTES = {
  _ROOT: "/configuration/lead-time-profile",
  EDIT: "/configuration/lead-time-profile/edit"
};
export const Organization_Routes = {
  _ROOT: "/configuration/organization",
  CREATE_ORG_UNIT: "/configuration/organization/create_org_unit",
  EDIT_ORG_UNIT: "/configuration/organization/create_org_unit/:id"
};

export const ORGANIZATION_USERS_ROUTES = {
  _ROOT: "/configuration/organization_users"
};
export const AUDIT_LOGS_ROUTES = {
  _ROOT: "/configuration/audit_logs"
};

export const WIDGET_HEADER_DISPLAY = {
  tickets_by_stage: "Ticket status",
  file_changes_count: "Files Changed",
  configs_count: "Configs Changed",
  commits_count: "Commits"
};

export const TRELLIS_SCORE_PROFILE_ROUTES = {
  _ROOT: "/configuration/trellis_score_profile",
  EDIT: "/configuration/trellis_score_profile/profile"
};

export const TRELLIS_SCORE_CENTRAL_PROFILE_ROUTES = {
  _ROOT: "/configuration/trellis_central_profile"
};

export const getHomePage = (params: ProjectPathProps) =>
  window.isStandaloneApp ? `${getBaseUrl()}/sei-home` : `${getBaseUrl()}/dashboards`;
export const getDashboardsPage = (params: ProjectPathProps) => `${getBaseUrl(params)}/dashboards`;
export const getReportsPage = () => `${getBaseUrl()}/reports`;
export const getWorkitemsPage = () => `${getBaseUrl()}/workitems`;
export const getSettingsPage = () => `${getBaseUrl()}/configuration`;
export const getIntegrationPage = () => `${getBaseUrl()}/configuration/integrations`;
export const getQuizAnswerPage = () => `${getBaseUrl()}/answer-questionnaire-page`;
export const getWorkitemDetailPage = () => `${getBaseUrl()}/workitems/details`;
export const getWorkflowProfilePage = () => `${getBaseUrl()}/configuration/lead-time-profile`;
export const getEmptyPage = () => `${getBaseUrl()}/not-authorized`;
export const getContributersPage = () => `${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`;
export const getInvestmentPage = () => `${getBaseUrl()}${TICKET_CATEGORIZATION_SCHEMES_ROUTES._ROOT}`;
export const getTrellisPage = () => `${getBaseUrl()}${TRELLIS_SCORE_PROFILE_ROUTES._ROOT}`;
export const getTrellisCentralPage = () => `${getBaseUrl()}${TRELLIS_SCORE_CENTRAL_PROFILE_ROUTES._ROOT}`;
export const getCollectionsPage = (params: ProjectPathProps) => `${getBaseUrl(params)}${Organization_Routes._ROOT}`;
export const getTablesPage = () => `${getBaseUrl()}${CONFIG_TABLE_ROUTES._ROOT}`;
export const getPropelsPage = () => `${getBaseUrl()}${PROPELS_ROUTES._ROOT}`;
export const getIntegrationMappingPage = () => `${getBaseUrl()}/sei-integration-mapping`;

export const DEFAULT_ROUTES = {
  ADMIN: getHomePage,
  LIMITED_USER: getReportsPage,
  AUDITOR: getReportsPage,
  RESTRICTED_USER: getEmptyPage,
  ASSIGNED_ISSUES_USER: getWorkitemsPage,
  PUBLIC_DASHBOARD: getDashboardsPage
};
