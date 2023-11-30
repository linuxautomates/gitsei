import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
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
import { TIME_RANGE_OPTIONS } from "./constant";
import { AcrossFilterConfig } from "./specific-filter-config.constant";

export const PrsSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig.filter((item: LevelOpsFilter) => item.beKey !== "branches"),
  CodeChangeSizeWrapperFilterConfig,
  CodeDensityWrapperFilterConfig,
  OtherCriteriaFilterConfig,
  AcrossFilterConfig,
  generateTimePeriodFilterConfig(TIME_RANGE_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
  NumberOfReviewersFilterConfig,
  NumberOfApproversFilterConfig,
  PrCommentDensityFilterConfig,
  CodeChangeSizeFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
