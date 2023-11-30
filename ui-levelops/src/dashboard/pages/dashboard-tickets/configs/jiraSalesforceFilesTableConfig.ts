import { baseColumnConfig } from "utils/base-table-config";
import { coloredTagsColumn, dateRangeFilterColumn, timeColumn } from "./common-table-columns";

export const JiraSalesforceFilesTableConfig = [
  baseColumnConfig("Repo", "repo_id"),
  baseColumnConfig("Filename", "filename"),
  coloredTagsColumn("Salesforce Cases", "salesforce_case_numbers"),
  baseColumnConfig("Num Commits", "num_commits"),
  baseColumnConfig("Total Deletions", "total_deletions"),
  baseColumnConfig("Total Additions", "total_additions"),
  baseColumnConfig("Total Changes", "total_changes"),
  {
    ...timeColumn(),
    sorter: false,
    width: "9%"
  },
  {
    ...dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    description: "Just to show the filters"
  },
  {
    ...dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at"),
    description: "Just to show the filters"
  }
];
