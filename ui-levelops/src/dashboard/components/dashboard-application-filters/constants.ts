import { OUDefaultUsersSettingConfigType } from "configurations/configuration-types/OUTypes";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const DASHBOARD_FILTER_INFO_TEXT =
  "Filter Jira data analyzed on the insight. Tickets matching ANY of the filters are selected. For example, if the insight is about a specific Project or Component then this is a good place to apply the filter. Widgets added to the insight can further filter the data to analyze specific metrics.";

export const DEFAULT_WIDGET_FILTER_INFO_TEXT =
  "Configure default values for commonly used widget filters on the insight. If most widgets on the insight, except for a handful, are about a specific issue type or project then this is a good place to configure the commonly used defaults.";

export const DEFAULT_OU_USERS_INFO_TEXT =
  "SEI uses default field for Collection based aggregations.\n You can override them here.";

export const ouApplicationsConfig: OUDefaultUsersSettingConfigType[] = [
  {
    application: IntegrationTypes.JIRA,
    options: [
      { label: "Assignee", value: "assignee" },
      { label: "Reporter", value: "reporter" }
    ]
  },
  {
    application: IntegrationTypes.GITHUB,
    options: [
      { label: "Repo", value: "repo_id" },
      { label: "Assignee", value: "assignee" },
      { label: "Reviewer", value: "reviewer" },
      { label: "Creator", value: "creator" },
      { label: "Author", value: "author" }
    ]
  },
  {
    application: IntegrationTypes.JENKINS,
    options: [{ label: "CI/CD Contributors", value: "cicd_user_id" }]
  },
  {
    application: IntegrationTypes.AZURE,
    options: [
      { label: "Workitem Project", value: "workitem_project" },
      { label: "Workitem Status", value: "workitem_status" },
      { label: "Workitem Priority", value: "workitem_priority" },
      { label: "Workitem Type", value: "workitem_type" },
      { label: "Workitem Status Category", value: "workitem_status_category" },
      { label: "Workitem Parent Workitem Id", value: "workitem_parent_workitem_id" },
      { label: "Workitem Epic", value: "workitem_epic" },
      { label: "Workitem Assignee", value: "workitem_assignee" },
      { label: "Workitem Version", value: "workitem_version" },
      { label: "Workitem Fix Version", value: "workitem_fix_version" },
      { label: "Workitem Reporter", value: "workitem_reporter" },
      { label: "Workitem Label", value: "workitem_label" }
    ]
  },
  {
    application: IntegrationTypes.PAGERDUTY,
    options: [
      { label: "Service", value: "pd_service" },
      { label: "Incident Priority", value: "incident_priority" },
      { label: "Incident Urgenct", value: "incident_urgency" },
      { label: "Alert Severity", value: "alert_severity" },
      { label: "User (Engineer)", value: "user_id" }
    ]
  }
];

export const OU_USER_FILTER_DESIGNATION_KEY = "ou_user_filter_designation";
