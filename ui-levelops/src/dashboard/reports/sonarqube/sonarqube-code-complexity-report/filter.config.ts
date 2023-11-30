import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { SonarqubeMetricsCommonFiltersConfig, sonarqubeCodeComplexityMetricsFilterConfig } from "dashboard/report-filters/sonarqube/sonarqube-metrics-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { METRIC_OPTIONS } from "./constants";
import { ComplexityRangeFilterConfig } from "./specific-filter-config.constants";

export const SonarqubeCodeComplexityReportFiltersConfig: LevelOpsFilter[] = [
  ...SonarqubeMetricsCommonFiltersConfig,
  ComplexityRangeFilterConfig,
  ShowValueOnBarConfig,
  sonarqubeCodeComplexityMetricsFilterConfig(METRIC_OPTIONS)
];
