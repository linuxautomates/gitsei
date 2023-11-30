export enum AnalyticsCategoryType {
  INTEGRATION = "integrations",
  WORKFLOW_PROFILES = "workflow_profiles",
  LOGIN_FLOW = "login_flow",
  DEMO_TOUR = "demo_tour",
  DATA_GLOBAL_FILTERS = "date_global_filter",
  WIDGETS = "widgets",
  WIDGET_FILTERS = "widget_filters",
  PROPELS = "propels",
  DASHBOARD = "dashboards",
  ORG_UNITS = "org_units"
}

export enum DashboardActions {
  CREATE_NEW = "created_new"
}

export enum IntegrationAnalyticsActions {
  ADD_INTEGRATIONS_BANNER_CLICK = "add_integrations_banner_click",
  INTEGRATION_APP_START = "integration_app_start",
  INTEGRATION_APP_SUCCESS = "integration_app_success",
  MODIFY_APP_INTEGRATION = "modify_app_integration"
}

export enum WorkflowProfileAnalyticsActions {
  ADD_PROFILE = "workflow_add_profile",
  DELETE_PROFILE = "workflow_delete_profile",
  CLONE_PROFILE = "workflow_clone_profile",
  ADD_STAGE = "workflow_add_new_stage",
  DELETE_STAGE = "workflow_delete_stage"
}

export enum LoginAnalyticsActions {
  LOGIN_SUCCESS = "successful_login"
}

export enum DemoTourActions {
  START = "start",
  CLOSE = "close"
}
export enum DemoTourActionsLabal {
  CLOSE = "tour elements viewed"
}

export enum DataGlobalFiltersActions {
  UPDATED = "updated"
}

export enum WidgetsActions {
  CREATED = "created"
}

export enum WidgetFiltersActions {
  FILTER_ADDED = "filter_added",
  FILTER_DELETED = "filter_deleted"
}

export enum PropelsActions {
  PROPEL_ADD = "propel_add"
}
export type AnalyticsActionType =
  | WorkflowProfileAnalyticsActions
  | WidgetFiltersActions
  | WidgetsActions
  | IntegrationAnalyticsActions
  | LoginAnalyticsActions
  | DemoTourActions
  | DataGlobalFiltersActions
  | PropelsActions
  | DashboardActions;

export const GA_ANALYTICS_NON_TRACKABLE_EMAILS = [
  "customersuccess@levelops.io",
  "customersuccess@propelo.ai",
  "sei-cs@harness.io"
];
