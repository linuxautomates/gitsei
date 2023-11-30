import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CrossAggregationGrpByModuleFilterConfig } from "../common/group-by-module-filter.config";
import { generateCrossAggregationModulePathFilterConfig } from "../common/module-path-filter.config";
import { supportManagementSystemFilterConfig } from "../common/supportManagementFilters.config";
import { jiraSalesforceCommonFiltersConfig } from "./common-filters.config";

export const JiraSalesforceSupportHotSpotReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraSalesforceCommonFiltersConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  supportManagementSystemFilterConfig,
  generateCrossAggregationModulePathFilterConfig("jira_salesforce_files_report"),
  CrossAggregationGrpByModuleFilterConfig
];
