export const FAILURE_RATE_URI = "scm_dora_failure_rate";

export const supportedFilters = {
  uri: "github_prs_filter_values",
  values: ["label", "project", "repo_id", "branch", "creator", "source_branch", "target_branch"]
};

export const FAILURE_RATE_DESCRIPTION =
  "The failure rate is defined as the percentage of deployments that lead to a production failure. For the Elite performing teams, the failure rate is less than 15%, the High is between 16 to 30%, the Medium is 31 to 45%, and the Low is anything greater than 45%.";

export const MESSAGE = "This version of the report is not supported. Please use the “Change Failure Rate” report.";
