import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export const scmDoraSupportedFilters = ["creator", "repo_id", "project"];

export const githubFiltersKeyMapping: Record<string, string> = {
  creator: "creators",
  repo_id: "repo_ids",
  project: "projects"
};

export const cicdSupportedFilters: supportedFilterType = {
  uri: "cicd_filter_values",
  values: ["cicd_user_id", "job_status", "instance_name"]
};

export const IMSupportedFilters: supportedFilterType = {
  uri: "filter_values",
  values: [
    "status",
    "priority",
    "issue_type",
    "assignee",
    "project",
    "component",
    "label",
    "reporter",
    "fix_version",
    "version",
    "resolution",
    "status_category"
  ]
};

export const IM_FILTERS_KEY_MAPPING: Record<string, string> = {
  status: "status",
  priority: "priorities",
  issue_type: "issue_types",
  assignee: "assignees",
  project: "projects",
  component: "components",
  label: "labels",
  reporter: "reporter",
  fix_version: "fix_version",
  version: "versions",
  resolution: "resolutions",
  status_category: "status_categories"
};
