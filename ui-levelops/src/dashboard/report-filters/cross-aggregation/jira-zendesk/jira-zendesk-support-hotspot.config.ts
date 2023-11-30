import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { CrossAggregationModulePathFilterConfig } from "../common/module-path-filter.config";
import { jiraZendeskCommonFiltersConfig } from "./common-filters.config";
import { supportManagementSystemFilterConfig } from "../common/supportManagementFilters.config";
import { CrossAggregationGrpByModuleFilterConfig } from "../common/group-by-module-filter.config";

export const JiraZendeskSupportHotSpotReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraZendeskCommonFiltersConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  supportManagementSystemFilterConfig,
  CrossAggregationModulePathFilterConfig,
  CrossAggregationGrpByModuleFilterConfig
];
