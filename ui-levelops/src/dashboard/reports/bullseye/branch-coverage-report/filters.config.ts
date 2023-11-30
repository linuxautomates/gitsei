import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { bullseyeCommonFiltersConfig, StackedFilterConfig } from "../bullseye-specific-filter-config.constant";
import { METRIC_OPTIONS } from "../constant";
import { ACROSS_OPTIONS } from "./constants";

export const BranchCoverageReportFiltersConfig: LevelOpsFilter[] = [
  ...bullseyeCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateMetricFilterConfig(METRIC_OPTIONS, "tags", "condition_percentage_coverage", "metric"),
  MaxRecordsFilterConfig,
  StackedFilterConfig,
  ShowValueOnBarConfig
];
