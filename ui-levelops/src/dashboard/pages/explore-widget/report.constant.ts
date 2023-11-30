import { ProjectPathProps } from "classes/routeInterface";
import { BULLSEYE_REPORTS, COVERITY_REPORTS, DORA_REPORTS, ISSUE_MANAGEMENT_REPORTS, JENKINS_REPORTS, MICROSOFT_ISSUES_REPORT_NAME, MICROSOFT_REPORT, NCC_GROUP_REPORTS, PRAETORIAN_REPORTS, SALESFORCE_REPORTS, SCM_REPORTS, SONARQUBE_REPORTS, SUPPORT_REPORTS, TESTRAILS_REPORTS, ZENDESK_REPORTS } from "dashboard/constants/applications/names";
import { WebRoutes } from "../../../routes/WebRoutes";
import { LEVELOPS_REPORTS } from "dashboard/reports/levelops/constant";

export interface ReportTheme {
  reports: string[];
  "report-categories": string[];
  content: string;
  description: string;
  id: string;
  "image-url"?: string;
}

export interface CategoryTheme {
  key: string;
  label: string;
  description: string;
  link: string;
  icon: string;
}

export const reportThemes = (params: ProjectPathProps, dashboardId: string): CategoryTheme[] => [
  {
    key: "dora",
    label: "DORA",
    description: "The Baseline Metrics for Engineering Excellence.",
    icon: "widgetThemeMiscellaneous",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCustomCategory(params, dashboardId, "dora")
  },
  {
    key: "effort_investment",
    label: "Effort Investment",
    description: "Check if engineering investments align with business goals.",
    icon: "widgetThemeBusiness",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "effort_investment")
  },
  {
    key: "velocity",
    label: "Velocity",
    description: "Identify bottlenecks in product development and delivery process.",
    icon: "widgetThemeVelocity",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "velocity")
  },
  {
    key: "quality",
    label: "Quality",
    description: "Identify hotspots and triangulate with customer impact and code coverage.",
    icon: "widgetThemeQuality",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "quality")
  },
  {
    key: "security",
    label: "Security",
    description: "Identify top issues, improve resolution time and security posture.",
    icon: "widgetThemeSecurity",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "security")
  },
  {
    key: "hygiene",
    label: "Hygiene",
    description: "Identify gaps in following engineering best practices around Jira and PRs",
    icon: "widgetThemeHygiene",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "hygiene")
  },
  {
    key: "customer_support",
    label: "Customer Support",
    description: "Identify customer impact of bugs and insights to improve customer satisfaction.",
    icon: "widgetThemeCustomerSupport",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "customer_support")
  },
  {
    key: "miscellaneous",
    label: "Miscellaneous",
    description: "Broad range of widgets for incident management, CICD and SEI issues",
    icon: "widgetThemeMiscellaneous",
    link: WebRoutes.dashboard.widgets.widgetsExploreByCategory(params, dashboardId, "miscellaneous")
  }
];

export const ALL_REPORTS = "ALL_REPORTS";

export const DISABLE_WIDGET_MESSAGE =
  "Please complete the configuration for the Workflow Profile in order to enable this option";

export const disableTooltipMessage = "This report cannot be configured due to missing integrations";

export const CUSTOMER_SUPPORT_ADVANCE_TAB_WIDGET_ARRAY = [
  SUPPORT_REPORTS.ZENDESK_C2F_TRENDS,
  SUPPORT_REPORTS.JIRA_ZENDESK_ESCALATION_TIME_REPORT,
  SUPPORT_REPORTS.ZENDESK_TIME_ACROSS_STAGES,
  ZENDESK_REPORTS.ZENDESK_AGENT_WAIT_TIME_REPORT_TRENDS,
  SUPPORT_REPORTS.ZENDESK_BOUNCE_REPORT,
  ZENDESK_REPORTS.ZENDESK_HOPS_REPORT_TRENDS,
  SUPPORT_REPORTS.ZENDESK_HYGIENE_REPORT,
  ZENDESK_REPORTS.ZENDESK_HYGIENE_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_REPLIES_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_REQUESTER_WAIT_TIME_REPORT,
  ZENDESK_REPORTS.ZENDESK_REQUESTER_WAIT_TIME_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_RESOLUTION_TIME_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_RESPONSE_TIME_REPORT,
  ZENDESK_REPORTS.ZENDESK_RESPONSE_TIME_TREND_REPORT,
  ZENDESK_REPORTS.ZENDESK_REOPENS_REPORT,
  ZENDESK_REPORTS.ZENDESK_REOPENS_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_TICKETS_REPORT_TRENDS
];

export const DORA_ADVANCE_TAB_WIDGET_ARRAY = [
  DORA_REPORTS.LEADTIME_CHANGES,
  DORA_REPORTS.MEANTIME_RESTORE
];

export const EFFORT_INVESTMENT_ADVANCE_TAB_WIDGET_ARRAY = [];

export const HYGIENE_ADVANCE_TAB_WIDGET_ARRAY = [
  SUPPORT_REPORTS.ZENDESK_HYGIENE_REPORT,
  ZENDESK_REPORTS.ZENDESK_HYGIENE_REPORT_TRENDS
];

export const MISCELLANEOUS_ADVANCE_TAB_WIDGET_ARRAY = [
  LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT_TRENDS,
  LEVELOPS_REPORTS.WORKITEM_COUNT_REPORT_TRENDS,
  SCM_REPORTS.ISSUES_FIRST_RESPONSE_TREND_REPORT
];

export const QUALITY_ADVANCE_TAB_WIDGET_ARRAY = [
  BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_BRANCH_COVERAGE_TREND_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_CODE_COVERAGE_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_CODE_COVERAGE_TREND_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_DECISION_COVERAGE_TREND_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_REPORT,
  BULLSEYE_REPORTS.BULLSEYE_FUNCTION_COVERAGE_TREND_REPORT,
  JENKINS_REPORTS.JOB_RUNS_TEST_DURATION_REPORT,
  SONARQUBE_REPORTS.SONARQUBE_CODE_DUPLICATION_TREND_REPORT,
  TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT,
  TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_TRENDS_REPORT,
  TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_REPORT,
  TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_TRENDS_REPORT
];

export const SECURITY_ADVANCE_TAB_WIDGET_ARRAY = [
  MICROSOFT_ISSUES_REPORT_NAME,
  NCC_GROUP_REPORTS.VULNERABILITY_REPORT,
  PRAETORIAN_REPORTS.PRAETORIAN_ISSUES_REPORT,
  COVERITY_REPORTS.COVERITY_ISSUE_REPORT,
  COVERITY_REPORTS.COVERITY_ISSUE_STAT_REPORT,
  COVERITY_REPORTS.COVERITY_ISSUE_TREND_REPORT
];

export const VELOCITY_ADVANCE_TAB_WIDGET_ARRAY = [
  JENKINS_REPORTS.JOB_CONFIG_CHANGE_COUNTS_STAT,
  SCM_REPORTS.ISSUES_FIRST_RESPONSE_REPORT,
  SCM_REPORTS.ISSUES_FIRST_RESPONSE_COUNT_SINGLE_STAT,
  ZENDESK_REPORTS.ZENDESK_REQUESTER_WAIT_TIME_REPORT,
  ZENDESK_REPORTS.ZENDESK_REQUESTER_WAIT_TIME_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_RESOLUTION_TIME_REPORT_TRENDS,
  ZENDESK_REPORTS.ZENDESK_RESPONSE_TIME_REPORT,
  ZENDESK_REPORTS.ZENDESK_RESPONSE_TIME_TREND_REPORT
];

export const ADVANCE_TAB_WIDGET_ARRAY = [
  ...CUSTOMER_SUPPORT_ADVANCE_TAB_WIDGET_ARRAY,
  ...DORA_ADVANCE_TAB_WIDGET_ARRAY,
  ...EFFORT_INVESTMENT_ADVANCE_TAB_WIDGET_ARRAY,
  ...HYGIENE_ADVANCE_TAB_WIDGET_ARRAY,
  ...MISCELLANEOUS_ADVANCE_TAB_WIDGET_ARRAY,
  ...QUALITY_ADVANCE_TAB_WIDGET_ARRAY,
  ...SECURITY_ADVANCE_TAB_WIDGET_ARRAY,
  ...VELOCITY_ADVANCE_TAB_WIDGET_ARRAY,
]