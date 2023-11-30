import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { scmPRsResponseTimeMetricsOptions } from "dashboard/graph-filters/components/Constants";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { TIME_RANGE_OPTIONS } from "../prs-single-stat/constant";
import {
  CodeChangeSizeWrapperFilterConfig,
  CodeDensityWrapperFilterConfig,
  NumberOfApproversFilterConfig,
  NumberOfReviewersFilterConfig,
  OtherCriteriaFilterConfig,
  PrCommentDensityFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  CodeChangeSizeFilterConfig
} from "../scm-specific-filter-config.constant";

export const PrsResponseTimeSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig,
  CodeChangeSizeWrapperFilterConfig,
  CodeDensityWrapperFilterConfig,
  OtherCriteriaFilterConfig,
  generateTimePeriodFilterConfig(TIME_RANGE_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
  NumberOfReviewersFilterConfig,
  NumberOfApproversFilterConfig,
  PrCommentDensityFilterConfig,
  CodeChangeSizeFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  generateMetricFilterConfig(scmPRsResponseTimeMetricsOptions, "default", "average_author_response_time"),
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
