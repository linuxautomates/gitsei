import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { SonarqubeCommonFiltersConfig } from "dashboard/report-filters/sonarqube/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { STACK_FILTERS } from "./constants";
import { StackFilterConfig } from "./specific-filter.config.constants";

export const SonarqubeIssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...SonarqubeCommonFiltersConfig,
  StackFilterConfig,
  ShowValueOnBarConfig,
  generateAcrossFilterConfig(STACK_FILTERS),
  MaxRecordsFilterConfig
];
