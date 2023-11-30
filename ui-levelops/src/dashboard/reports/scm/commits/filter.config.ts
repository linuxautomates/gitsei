import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  CodeChangeSizeFilterConfig,
  CodeChangeSizeWrapperFilterConfig,
  CommittedInFilterConfig,
  FileTypeFilterConfig
} from "../scm-specific-filter-config.constant";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { ACROSS_OPTIONS, VISUALIZATION_OPTIONS } from "./constant";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { getAcrossValue } from "./helper";

export const CommitsReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  { ...generateAcrossFilterConfig(ACROSS_OPTIONS), getMappedValue: getAcrossValue },
  generateVisualizationFilterConfig(VISUALIZATION_OPTIONS, IssueVisualizationTypes.PIE_CHART, false),
  MaxRecordsFilterConfig,
  CodeChangeSizeWrapperFilterConfig,
  FileTypeFilterConfig,
  CommittedInFilterConfig,
  CodeChangeSizeFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
