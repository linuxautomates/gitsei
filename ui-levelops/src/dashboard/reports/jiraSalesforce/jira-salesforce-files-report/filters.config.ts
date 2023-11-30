import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateIssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { CrossAggregationGrpByModuleFilterConfig } from "dashboard/report-filters/cross-aggregation/common/group-by-module-filter.config";
import { generateCrossAggregationModulePathFilterConfig } from "dashboard/report-filters/cross-aggregation/common/module-path-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { jiraSalesforceCommonFiltersConfig } from "dashboard/report-filters/cross-aggregation/jira-salesforce/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const JiraSalesforceSupportHotSpotReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraSalesforceCommonFiltersConfig,
  generateIssueCreatedAtFilterConfig([], "", "jira_issue_created_at"),
  generateIssueUpdatedAtFilterConfig("jira_issue_updated_at"),
  supportManagementSystemFilterConfig,
  generateCrossAggregationModulePathFilterConfig("jira_salesforce_files_report"),
  CrossAggregationGrpByModuleFilterConfig
];
