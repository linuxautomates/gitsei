import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { SonarqubeMetricsCommonFiltersConfig, sonarqubeCodeComplexityMetricsFilterConfig } from "dashboard/report-filters/sonarqube/sonarqube-metrics-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { METRIC_OPTIONS } from "./constants";
import { ComplexityRangeFilterConfig } from "./specific-filter-config.constants";

export const SonarqubeCodeComplexityTrendsReportFiltersConfig: LevelOpsFilter[] = [
  ...SonarqubeMetricsCommonFiltersConfig,
  ComplexityRangeFilterConfig,
  sonarqubeCodeComplexityMetricsFilterConfig(METRIC_OPTIONS)
];
