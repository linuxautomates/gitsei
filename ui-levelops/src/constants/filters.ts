export const FILTER_TYPE = {
  MULTI_SELECT: "multiSelect",
  SELECT: "select",
  API_SELECT: "apiSelect",
  API_MULTI_SELECT: "apiMultiSelect",
  DATE_RANGE: "dateRange",
  SEARCH: "search",
  INPUT: "input",
  TAGS: "tags",
  CASCADE: "cascade",
  FE_SELECT: "feSelect",
  PARTIAL_MATCH: "partial_match",
  BINARY: "binary"
};

export const ID_FILTERS = ["assignee", "reporter", "first_assignee"];

export const GROUP_BY_TIME_FILTERS = ["issue_created", "issue_updated", "issue_resolved", "issue_due", "issue_closed"];
export const AZURE_TIME_FILTERS_KEYS = ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at"];
export const PAGER_DUTY_TIME_FILTERS_KEYS = [
  "alert_created_at",
  "incident_resolved_at",
  "incident_created_at",
  "alert_resolved_at"
];
export const SCM_PRS_TIME_FILTERS_KEYS = ["pr_closed", "pr_created"];
export const PAGERDUTY_TIME_FILTER_KEYS = [
  "incident_created_at",
  "incident_resolved_at",
  "alert_created_at",
  "alert_resolved_at"
];
export const JENKINS_TIME_FILTER_KEYS = ["job_end"];
export const TIME_FILTERS_KEYS = [
  ...GROUP_BY_TIME_FILTERS,
  ...AZURE_TIME_FILTERS_KEYS,
  ...PAGER_DUTY_TIME_FILTERS_KEYS,
  ...SCM_PRS_TIME_FILTERS_KEYS,
  ...PAGERDUTY_TIME_FILTER_KEYS,
  ...JENKINS_TIME_FILTER_KEYS,
  "trend",
  "ticket_created" // zendesk
];
export const IGNORE_X_AXIS_KEYS = ["NA"];
