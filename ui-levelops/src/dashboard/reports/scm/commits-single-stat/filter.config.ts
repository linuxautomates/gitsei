import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateTimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  CodeChangeSizeFilterConfig,
  CodeChangeSizeWrapperFilterConfig,
  CommittedInFilterConfig
} from "../scm-specific-filter-config.constant";
import { TIME_RANGE_OPTIONS } from "./constant";

export const CommitsSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  CodeChangeSizeWrapperFilterConfig,
  generateTimePeriodFilterConfig(TIME_RANGE_OPTIONS, { required: true }),
  { ...AggregationTypesFilterConfig, required: true },
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
