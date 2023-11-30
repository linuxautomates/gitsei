import { SonarqubeMetricsCommonFiltersConfig } from "dashboard/report-filters/sonarqube/sonarqube-metrics-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const SonarqubeCodeDuplicationTrendsReportFiltersConfig: LevelOpsFilter[] = [
  ...SonarqubeMetricsCommonFiltersConfig
];
