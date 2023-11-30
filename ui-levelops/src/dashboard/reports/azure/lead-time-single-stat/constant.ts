export const CALCULATION_OPTIONS = [
  { value: "ticket_velocity", label: "Average Lead Time per Ticket" },
  { value: "pr_velocity", label: "Average Lead Time per PR" }
];

export const githubOptions = [
  { label: "Repo", value: "repo_id" },
  {
    label: "Assignee",
    value: "assignee"
  },
  {
    label: "Approver",
    value: "approver"
  },
  {
    label: "Author",
    value: "author"
  },
  {
    label: "Reviewer",
    value: "reviewer"
  },
  {
    label: "Committer",
    value: "committer"
  }
];

export const REPORT_NAME = "Lead Time Single Stat";
export const URI = "lead_time_report";
export const FILTERS = {
  across: "velocity"
};
export const CHART_PROPS = {
  unit: "Days"
};
export const COMPARE_FIELD = "mean";

export const DEFAULT_QUERY = {
  calculation: "ticket_velocity"
};
