export const FILTER_WITH_INFO_MAPPING = "filterWithInfoMapping";

export type filterWithInfoType = { id: string; label: string; description: string; filterKey: string };

export const jiraHideStatusFilter: filterWithInfoType = {
  id: "hide-status",
  label: "Hide Status",
  filterKey: "stages",
  description:
    "Hide selected status values from the graph. It is recommended to hide terminal statuses like Done, Won't Do."
};

export const azureHideStatusFilter: filterWithInfoType = {
  id: "hide-status",
  label: "Hide Status",
  filterKey: "workitem_stages",
  description:
    "Hide selected status values from the graph. It is recommended to hide terminal statuses like Done, Won't Do."
};

export const jiraExcludeStatusFilter: filterWithInfoType = {
  id: "exclude-status",
  label: "Exclude Time in Status",
  filterKey: "stages",
  description: "Exclude time spent in the selected Jira states from resolution time"
};

export const azureExcludeStatusFilter: filterWithInfoType = {
  id: "exclude-status",
  label: "Exclude Time in Status",
  filterKey: "workitem_stages",
  description: "Exclude time spent in the selected Azure states from resolution time"
};

export const scmExcludeStatusFilter: filterWithInfoType = {
  id: "exclude-status",
  label: "Exclude Time in Status",
  filterKey: "stages",
  description: "Exclude time spent in the selected Jira states from resolution time"
};

export const leadTimeExcludeStageFilter: filterWithInfoType = {
  id: "exclude-stages",
  label: "Exclude Stages",
  filterKey: "jira_stages",
  description: "Exclude selected stages from Lead Time computation"
};

export const sccIssueExcludeStatusFilter: filterWithInfoType = {
  id: "exclude-status",
  label: "Exclude Status",
  filterKey: "columns",
  description:
    "Exclude selected statuses from Cycle Time computation. It is recommended to exclude terminal states like Done and Won't Do."
};
