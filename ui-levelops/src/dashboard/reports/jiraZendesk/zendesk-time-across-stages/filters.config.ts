import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateIssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { jiraZendeskCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-zendesk/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { StateTrantitionFilterConfig } from "./specific-filter-config.constant";

export const ZendeskTimeAcrossStagesReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraZendeskCommonFiltersConfig,
  generateIssueCreatedAtFilterConfig([], "", "jira_issue_created_at"),
  generateIssueUpdatedAtFilterConfig("jira_issue_updated_at"),
  StateTrantitionFilterConfig,
  supportManagementSystemFilterConfig
];
