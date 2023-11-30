import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { CrossAggregationGrpByModuleFilterConfig } from "dashboard/report-filters/cross-aggregation/common/group-by-module-filter.config";
import { generateCrossAggregationModulePathFilterConfig } from "dashboard/report-filters/cross-aggregation/common/module-path-filter.config";
import { ScmJiraCommonFiltersConfig } from "dashboard/report-filters/github/scm-jira-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { modulePathFilterSelectedHelper, modulePathFilterVisibilityHelper } from "./helper";

export const ScmJiraFilesFiltersConfig: LevelOpsFilter[] = [
  ...ScmJiraCommonFiltersConfig,
  generateCrossAggregationModulePathFilterConfig(
    "scm_jira_files_root_folder_report",
    modulePathFilterVisibilityHelper,
    modulePathFilterSelectedHelper
  ),
  CrossAggregationGrpByModuleFilterConfig,
  generateOUFilterConfig({
    github: {
      options: OUFiltersMapping.github
    }
  })
];
