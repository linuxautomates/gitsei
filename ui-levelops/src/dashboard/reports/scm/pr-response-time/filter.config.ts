import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { getSCMPrsTimeAcrossValue } from "../scm-prs-common.helper";
import {
  CodeChangeSizeFilterConfig,
  CodeChangeSizeWrapperFilterConfig,
  CodeDensityWrapperFilterConfig,
  NumberOfApproversFilterConfig,
  NumberOfReviewersFilterConfig,
  OtherCriteriaFilterConfig,
  PrClosedTimeFilterConfig,
  PrCommentDensityFilterConfig,
  PrCreatedInFilterConfig
} from "../scm-specific-filter-config.constant";
import { ACROSS_OPTIONS, METRIC_OPTIONS, VISUALIZATION_OPTIONS } from "./constant";
import { StackFilterConfig } from "./helper";

export const PrsResponseTimeReportFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig,
  { ...generateAcrossFilterConfig(ACROSS_OPTIONS), getMappedValue: getSCMPrsTimeAcrossValue },
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "average_author_response_time"),
  generateVisualizationFilterConfig(VISUALIZATION_OPTIONS, "pie_chart"),
  MaxRecordsFilterConfig,
  CodeChangeSizeWrapperFilterConfig,
  CodeDensityWrapperFilterConfig,
  OtherCriteriaFilterConfig,
  NumberOfReviewersFilterConfig,
  NumberOfApproversFilterConfig,
  PrCommentDensityFilterConfig,
  CodeChangeSizeFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  SortXAxisFilterConfig,
  StackFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
