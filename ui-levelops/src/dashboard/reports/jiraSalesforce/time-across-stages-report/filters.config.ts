import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateIssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { jiraSalesforceCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce/common-filters.config";
import { StateTrantitionFilterConfig } from "dashboard/reports/jiraZendesk/zendesk-time-across-stages/specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JiraSalesforceTimeAcrossStageReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraSalesforceCommonFiltersConfig,
  generateIssueCreatedAtFilterConfig([], "", "jira_issue_created_at"),
  generateIssueUpdatedAtFilterConfig("jira_issue_updated_at"),
  StateTrantitionFilterConfig,
  supportManagementSystemFilterConfig
];
