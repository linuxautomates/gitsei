import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { bullseyeCommonFiltersConfig } from "../bullseye-specific-filter-config.constant";
import { METRIC_OPTIONS } from "../constant";

export const FunctionCoverageTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...bullseyeCommonFiltersConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "condition_percentage_coverage", "metric")
];
