import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { jiraZendeskCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-zendesk/common-filters.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";

export const JiraZendeskSupportEscalationReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraZendeskCommonFiltersConfig,
  { ...IssueCreatedAtFilterConfig, beKey: "created_at" },
  supportManagementSystemFilterConfig
];
