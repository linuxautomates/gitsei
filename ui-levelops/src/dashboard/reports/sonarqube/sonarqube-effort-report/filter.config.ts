import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { SonarqubeCommonFiltersConfig } from "dashboard/report-filters/sonarqube/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";

export const SonarqubeEffortReportFiltersConfig: LevelOpsFilter[] = [
  ...SonarqubeCommonFiltersConfig,
  ShowValueOnBarConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig
];
