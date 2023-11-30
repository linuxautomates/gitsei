import { AggregationTypesFilterConfig } from "dashboard/report-filters/common/aggregation-type-filter.config";
import { TimePeriodFilterConfig } from "dashboard/report-filters/common/time-period-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  coverityCommonFiltersConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig
} from "../coverity-specific-filter-config.constant";

export const IssueSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...coverityCommonFiltersConfig,
  TimePeriodFilterConfig,
  AggregationTypesFilterConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig
];
