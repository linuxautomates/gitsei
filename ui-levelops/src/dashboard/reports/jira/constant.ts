import { makeObjectKeyAsValue } from "utils/commonUtils";

export const JIRA_FILTER_KEY_MAPPING: Record<string, string> = {
  status: "statuses",
  priority: "priorities",
  issue_type: "issue_types",
  assignee: "assignees",
  project: "projects",
  component: "components",
  hygiene_type: "hygiene_types",
  status_category: "status_categories",
  epic: "epics",
  fix_version: "fix_versions",
  version: "versions",
  reporter: "reporters",
  resolution: "resolutions",
  label: "labels",
  stage: "stages"
};

export const JIRA_ID_FILTER_KEY_MAPPING: Record<string, string> = {
  reporter: "reporters",
  assignee: "assignees"
};

export const JIRA_REVERSE_FILTER_KEY_MAPPING: Record<string, string> = makeObjectKeyAsValue(JIRA_FILTER_KEY_MAPPING);

export const JIRA_ID_REVERSE_FILTER_KEY_MAPPING: Record<string, string> =
  makeObjectKeyAsValue(JIRA_ID_FILTER_KEY_MAPPING);

export const JIRA_PARTIAL_FILTER_KEY_MAPPING: Record<string, string> = {
  component: "components",
  version: "versions",
  fix_version: "fix_versions",
  label: "labels"
};

export const LEAD_TIME_VALUES_TO_FILTERS_KEY = {
  jira_fix_version: "jira_fix_versions"
};

export const JIRA_COMMON_FILTER_LABEL_MAPPING: Record<string, string> = {
  version: "Affects Version"
};

export const WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY = "WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY";
export const WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPINGS = {
  started_at: "Sprint Start in",
  planned_ended_at: "Planned Sprint End in",
  completed_at: "Sprint Completed in"
};

export const requiredOneFiltersKeys = ["started_at", "completed_at", "planned_ended_at"];
