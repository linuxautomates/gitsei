export const TIME_TO_RECOVER_URI = "scm_dora_time_to_recover";

export const supportedFilters = {
  uri: "github_prs_filter_values",
  values: ["label", "project", "repo_id", "branch", "creator", "source_branch", "target_branch"]
};

export const TIME_TO_RECOVER_DESCRIPTION =
  "Mean time to restore is a measure of how long it takes a team to recover from a failure in production. For the Elite performing teams, MTTR is less than 1 hour, the High is less than 1 day, the Medium is less than 1 week, and the Low is more than a week.";
