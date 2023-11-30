import {
  DASHBOARD_ID_IN_HEADERS_ROLES,
  USERROLES,
  USER_ADMIN_ROLES,
  USER_NON_ADMIN_ROLES,
  WIDGET_EXTRAS_ROLES
} from "routes/helper/constants";

/** Add permissions here which are user role dependent */
export enum PermeableMetrics {
  ORG_PIVOT_CREATE = "ORG_PIVOT_CREATE",
  ORG_PIVOT_UPDATE = "ORG_PIVOT_UPDATE",
  ORG_PIVOT_SHOW = "ORG_PIVOT_SHOW",
  ORG_UNIT_CREATE = "ORG_UNIT_CREATE",
  ORG_UNIT_UPDATE = "ORG_UNIT_UPDATE",
  ORG_UNIT_LIST_PAGE_ACTIONS = "ORG_UNIT_LIST_PAGE_ACTIONS",
  ORG_UNIT_LIST_TREE_VIEW = "ORG_UNIT_LIST_TREE_VIEW",
  WORKITEM_ASSESSMENT_DELETE = "WORKITEM_ASSESSMENT_DELETE",
  ASSESSMENT_TABLE_DELETE_ACTION = "ASSESSMENT_TABLE_DELETE_ACTION",
  MFA_ENABLE_DISABLED_CONFIGURATION = "MFA_ENABLE_DISABLED_CONFIGURATION",
  SMART_TICKET_HEADER_ACTIONS = "SMART_TICKET_HEADER_ACTIONS",
  SMART_TICKET_PROJECT_UPDATE = "SMART_TICKET_PROJECT_UPDATE",
  ISSUE_CREATE_TAG_CREATION = "ISSUE_CREATE_TAG_CREATION",
  DASHBOARD_CREATE = "DASHBOARD_CREATE",
  DASHBOARD_LIST_ROW_SELECTION = "DASHBOARD_LIST_ROW_SELECTION",
  DASHBOARD_LIST_ACTIONS = "DASHBOARD_LIST_ACTIONS",
  DASHBOARD_HEADER_ACTIONS = "DASHBOARD_HEADER_ACTIONS",
  DASHBOARD_LIST_DROPDOWN_ACTIONS = "DASHBOARD_LIST_DROPDOWN_ACTIONS",
  DASHBOARD_CONFIGURATION_ACTION_BUTTONS = "DASHBOARD_CONFIGURATION_ACTION_BUTTONS",
  OPEN_REPORT_SET_DASHBOARD_ID_ACTION = "OPEN_REPORT_SET_DASHBOARD_ID_ACTION",
  WIDGET_LIST_ACTION_BUTTONS = "WIDGET_LIST_ACTION_BUTTONS",
  DASHBOARD_SEARCH_DROPDOWN_ACTIONS = "DASHBOARD_SEARCH_DROPDOWN_ACTIONS",
  ADD_DASHBOARD_ID_TO_LOCALSTORAGE = "ADD_DASHBOARD_ID_TO_LOCALSTORAGE",
  TRIAGE_FILTER_ACTIONS = "TRIAGE_FILTER_ACTIONS",
  DRILLDOWN_COLUMNS_JIRA_TICKETS_URL = "DRILLDOWN_COLUMNS_JIRA_TICKETS_URL",
  DASHBOARD_SET_DEFAULT_ROUTE = "DASHBOARD_SET_DEFAULT_ROUTE",
  MANAGE_WORKSPACE = "MANAGE_WORKSPACE",
  ADMIN_WIDGET_EXTRAS = "ADMIN_WIDGET_EXTRAS",
  DORA_WIDGET_EXTRAS = "DORA_WIDGET_EXTRAS",
  EFFORT_INVESTMENT_READ_ONLY = "EFFORT_INVESTMENT_READ_ONLY",
  TRELLIS_PROFILE_READ_ONLY = "TRELLIS_PROFILE_READ_ONLY",
  WORKFLOW_PROFILE_READ_ONLY = "WORKFLOW_PROFILE_READ_ONLY",
  ORG_UNIT_READ_ONLY = "ORG_UNIT_READ_ONLY",
  ORG_UNIT_USER_READ_ONLY = "ORG_UNIT_USER_READ_ONLY",
  NEW_RBAC_PERMISSION = "NEW_RBAC_PERMISSION",
  TRELLIS_CENTRAL_PROFILE_ACCESS = "TRELLIS_CENTRAL_PROFILE_ACCESS"
}

/**  Add allowed roles to each permission defined above */
export const PERMEABLE_METRICS_USERROLES_MAPPING: Record<PermeableMetrics, Array<USERROLES>> = {
  [PermeableMetrics.ORG_PIVOT_CREATE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ORG_PIVOT_UPDATE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ORG_UNIT_LIST_TREE_VIEW]: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.ORG_UNIT_CREATE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ORG_UNIT_UPDATE]: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.ORG_UNIT_LIST_PAGE_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.WORKITEM_ASSESSMENT_DELETE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ASSESSMENT_TABLE_DELETE_ACTION]: USER_ADMIN_ROLES,
  [PermeableMetrics.MFA_ENABLE_DISABLED_CONFIGURATION]: USER_ADMIN_ROLES,
  [PermeableMetrics.SMART_TICKET_HEADER_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.SMART_TICKET_PROJECT_UPDATE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ISSUE_CREATE_TAG_CREATION]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_CREATE]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_LIST_ROW_SELECTION]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_LIST_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_HEADER_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_CONFIGURATION_ACTION_BUTTONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.OPEN_REPORT_SET_DASHBOARD_ID_ACTION]: [...USER_ADMIN_ROLES, USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.WIDGET_LIST_ACTION_BUTTONS]: WIDGET_EXTRAS_ROLES,
  [PermeableMetrics.DASHBOARD_LIST_DROPDOWN_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_SEARCH_DROPDOWN_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE]: DASHBOARD_ID_IN_HEADERS_ROLES,
  [PermeableMetrics.TRIAGE_FILTER_ACTIONS]: USER_ADMIN_ROLES,
  [PermeableMetrics.DRILLDOWN_COLUMNS_JIRA_TICKETS_URL]: USER_NON_ADMIN_ROLES,
  [PermeableMetrics.DASHBOARD_SET_DEFAULT_ROUTE]: DASHBOARD_ID_IN_HEADERS_ROLES,
  [PermeableMetrics.ORG_PIVOT_SHOW]: USER_NON_ADMIN_ROLES,
  [PermeableMetrics.MANAGE_WORKSPACE]: USER_ADMIN_ROLES,
  [PermeableMetrics.ADMIN_WIDGET_EXTRAS]: USER_ADMIN_ROLES,
  [PermeableMetrics.DORA_WIDGET_EXTRAS]: WIDGET_EXTRAS_ROLES,
  [PermeableMetrics.EFFORT_INVESTMENT_READ_ONLY]: [USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.TRELLIS_PROFILE_READ_ONLY]: [USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.WORKFLOW_PROFILE_READ_ONLY]: [USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.ORG_UNIT_READ_ONLY]: [USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.ORG_UNIT_USER_READ_ONLY]: [USERROLES.ORG_UNIT_ADMIN],
  [PermeableMetrics.NEW_RBAC_PERMISSION]: [USERROLES.ORG_UNIT_ADMIN, USERROLES.PUBLIC_DASHBOARD],
  [PermeableMetrics.TRELLIS_CENTRAL_PROFILE_ACCESS]: USER_ADMIN_ROLES
};
