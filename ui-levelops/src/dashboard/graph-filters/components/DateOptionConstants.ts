export const timeBoundFilterKeys = [
  "issue_created_at",
  "issue_updated_at",
  "jira_issue_created_at",
  "jira_issue_updated_at",
  "pr_created_at",
  "pr_closed_at",
  "committed_at",
  "created_at",
  "salesforce_created_at",
  "salesforce_updated_at",
  "time_range",
  "ingested_at",
  "issue_resolved_at",
  "updated_at",
  "created_after",
  "completed_at",
  "disclosure_range",
  "publication_range",
  "snapshot_range",
  "jira_issue_resolved_at",
  "cicd_job_run_end_time",
  "pr_merged_at",
  "workitem_created_at",
  "workitem_updated_at",
  "workitem_resolved_at",
  "started_at",
  "planned_ended_at",
  "released_end_time"
];

export const closedDateOptions = [
  {
    id: "last_7_days",
    label: "Last 7 Days",
    lowerLimit: 6,
    upperLimit: 7
  },
  {
    id: "last_4_weeks",
    label: "Last 4 Weeks",
    lowerLimit: 21,
    upperLimit: 28
  },
  {
    id: "last_4_months",
    label: "Last 4 Months",
    lowerLimit: 90,
    upperLimit: 123
  },
  {
    id: "last_4_quarters",
    label: "Last 4 Quarters",
    lowerLimit: 275,
    upperLimit: 366
  }
];

export const dateTimeBoundFilterKeys = [
  "start_time",
  "end_time",
]