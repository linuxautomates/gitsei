export const SORT_OPTIONS = [
  { label: "Num Commits", value: "num_commits" },
  { label: "Changes", value: "changes" },
  { label: "Deletions", value: "deletions" },
  { label: "Additions", value: "additions" }
];

export const SCM_FILES_REPORT_DESCRIPTION =
  "Repos which have the highest count of changes (added or removed lines of code). Used to make sure that hottest code areas have good test coverage.";

export const REPORT_FILTERS = {
  across: "repo_id"
};
