export enum Entitlement {
  DASHBOARDS = "DASHBOARDS",
  DASHBOARDS_READ = "DASHBOARDS_READ",
  DASHBOARDS_COUNT_3 = "DASHBOARDS_COUNT_3",
  PROPELS = "PROPELS",
  PROPELS_READ = "PROPELS_READ",
  ALL_FEATURES = "ALL_FEATURES",
  ISSUES = "ISSUES",
  ISSUES_READ = "ISSUES_READ", // view issues
  PROPELS_COUNT_5 = "PROPELS_COUNT_5", // add, remove and modify propels with a limit of 5
  TRIAGE = "TRIAGE",
  TRIAGE_READ = "TRIAGE_READ",
  TEMPLATES = "TEMPLATES",
  REPORTS = "REPORTS",
  REPORTS_READ = "REPORTS_READ",
  TABLES = "TABLES",
  TABLES_READ = "TABLES_READ",
  //--- settings entitlements
  SETTINGS = "SETTINGS", // view and change all settings
  SETTINGS_READ = "SETTINGS_READ", // view settings
  SETTING_SSO = "SETTING_SSO", // allow configuring and enabling SSO
  SETTING_DEV_PRODUCTIVITY = "SETTING_DEV_PRODUCTIVITY", // edit dev productivity profile
  SETTING_DEV_PRODUCTIVITY_PROFILE_COUNT_3 = "SETTING_DEV_PRODUCTIVITY_PROFILE_COUNT_3", // edit dev productivity profiles with a limit of 3 profiles
  SETTING_DEV_PRODUCTIVITY_READ = "SETTING_DEV_PRODUCTIVITY_READ", // view dev productivity profile
  SETTING_EFFORT_INVESTMENT = "SETTING_EFFORT_INVESTMENT", // edit effort investment profiles
  SETTING_EFFORT_INVESTMENT_READ = "SETTING_EFFORT_INVESTMENT_READ", // view effort investment profiles
  SETTING_EFFORT_INVESTMENT_PROFILE_COUNT_3 = "SETTING_EFFORT_INVESTMENT_PROFILE_COUNT_3", // edit effort investment profiles with a limit of 3 profiles
  SETTING_WORKFLOW = "SETTING_WORKFLOW", // edit workflow profiles
  SETTING_WORKFLOW_READ = "SETTING_WORKFLOW_READ", // view workflow profiles
  SETTING_WORKFLOW_PROFILE_COUNT_3 = "SETTING_WORKFLOW_PROFILE_COUNT_3", // edit workflow profiles with a limit of 3 profiles
  SETTING_API_KEYS = "SETTING_API_KEYS", // create and remove api keys
  SETTING_API_KEYS_READ = "SETTING_API_KEYS_READ", // view api keys
  SETTING_API_KEYS_COUNT_2 = "SETTING_API_KEYS_COUNT_2", // create and remove api keys up to 2
  SETTING_ORG_UNITS = "SETTING_ORG_UNITS",
  SETTING_ORG_UNITS_READ = "SETTING_ORG_UNITS_READ",
  SETTING_ORG_UNITS_COUNT_5 = "SETTING_ORG_UNITS_COUNT_5",
  SETTING_USERS_COUNT_10 = "SETTING_USERS_COUNT_10",
  SETTING_SCM_INTEGRATIONS = "SETTING_SCM_INTEGRATIONS", // allow SCM integrations (no limits)
  SETTING_SCM_INTEGRATIONS_COUNT_3 = "SETTING_SCM_INTEGRATIONS_COUNT_3", // allow SCM integrations but only 3
  DROPDOWN_ADD_DYNAMIC_VALUE = "DROPDOWN_ADD_DYNAMIC_VALUE",
  NEW_DORA_LEADTIME_WIDGET = "NEW_DORA_LEADTIME_WIDGET", // New Dora widget

  // Allow user to add new Dora Lead time for change and MTTR reports
  LTFC_MTTR_DORA_IMPROVEMENTS = "LTFC_MTTR_DORA_IMPROVEMENTS",

  // Allows user to create ORG_ADMIN_USER and use it to manage OUs,
  ORG_UNIT_ENHANCEMENTS = "ORG_UNIT_ENHANCEMENTS",

  // Allows user to create MERGE USER through UI
  ORG_MERGE_USER_ENHANCEMENTS = "ORG_MERGE_USER_ENHANCEMENTS",

  // Allows user to add not splited azure integration in profile
  ALLOW_AZURE_NON_SPLIT_INTEGRATION = "ALLOW_AZURE_NON_SPLIT_INTEGRATION",

  // Allows jira relese profile in velocity profile,
  VELOCITY_JIRA_RELEASE_PROFILE = "VELOCITY_JIRA_RELEASE_PROFILE",

  // Show new trelis supported intervals
  SHOW_TRELIS_NEW_INTERVAL = "SHOW_TRELIS_NEW_INTERVAL",

  // USE IN DORA DF WIDGTE ON CLICK OF WEEK, DAY, MONTH BUTTON FOR GET DATA FROM KEY OTHER WISE CALCULATE FROM FE
  DF_CONFIGURABLE_WEEK_MONTH = "DF_CONFIGURABLE_WEEK_MONTH",

  // USE IN EFFORT INVESTMENT PROFILE FOR Y AXIS LABLE
  EFFORT_INVESTMENT_TREND_REPORT_YAXIS = "EFFORT_INVESTMENT_TREND_REPORT_YAXIS",

  // The last bar, current allocation is removed from api and UI (SEI-2802). Use this entitlement to enable it
  BA_TREND_CURRENT_ALLOCATION_DISABLED = "BA_TREND_CURRENT_ALLOCATION_DISABLED",

  // This moves custom fields and hygiene into their own tabs, performance improvements for hygiene /values as well
  CUSTOMFIELDS_HYGIENE_AS_TABS = "CUSTOMFIELDS_HYGIENE_AS_TABS",

  // this is for the allow github action tile
  ALLOW_GITHUB_ACTION_TILE = "ALLOW_GITHUB_ACTION_TILE",
  // New Trelis profile under OU and central profile for trelis changes
  TRELLIS_BY_JOB_ROLES = "TRELLIS_BY_JOB_ROLES"
}

export type EntitlementTypes = (typeof Entitlement)[keyof typeof Entitlement];

export enum EntitlementCheckType {
  OR = "OR",
  AND = "AND"
}

export const TOOLTIP_ACTION_NOT_ALLOWED = "This action is not allowed";
