import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { CrossAggregationGrpByModuleFilterConfig } from "dashboard/report-filters/cross-aggregation/common/group-by-module-filter.config";
import { generateCrossAggregationModulePathFilterConfig } from "dashboard/report-filters/cross-aggregation/common/module-path-filter.config";
import { githubFilesCommonFiltersConfig } from "dashboard/report-filters/github/github-files-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { scmFilesmodulePathFilterSelectedHelper, scmFilesmodulePathFilterVisibilityHelper } from "./helper";
import { SortFilterConfig } from "./specific-filter-config.constants";

export const FilesReportFiltersConfig: LevelOpsFilter[] = [
  ...githubFilesCommonFiltersConfig,
  generateCrossAggregationModulePathFilterConfig(
    "scm_files_root_folder_report",
    scmFilesmodulePathFilterVisibilityHelper,
    scmFilesmodulePathFilterSelectedHelper
  ),
  CrossAggregationGrpByModuleFilterConfig,
  generateIssueCreatedAtFilterConfig([], "Committed In", "committed_at"),
  SortFilterConfig,
  generateOUFilterConfig({
    github: {
      options: OUFiltersMapping.github
    }
  })
];
