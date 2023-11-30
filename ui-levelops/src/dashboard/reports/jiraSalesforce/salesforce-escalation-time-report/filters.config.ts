import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateIssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { jiraSalesforceCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JiraSalesforceEscalationTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraSalesforceCommonFiltersConfig,
  generateIssueCreatedAtFilterConfig([], "", "jira_issue_created_at"),
  generateIssueUpdatedAtFilterConfig("jira_issue_updated_at"),
  supportManagementSystemFilterConfig
];
