import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { PrClosedTimeFilterConfig, PrCreatedInFilterConfig } from "../scm-specific-filter-config.constant";
import { TIME_PERIOD_OPTIONS } from "./constant";
export const excludeFiltersForScmReportsConfig = ["branches", "labels"];

export const PrsMergeTrendSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  generateTimePeriodFilterConfig(TIME_PERIOD_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
