import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";
import {
  IngestedInFilterConfig,
  LastReportFilterConfig,
  praetorianCommonFiltersConfig,
  StackFilterConfig
} from "./specific-filter-config.constant";

export const IssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...praetorianCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig,
  StackFilterConfig,
  IngestedInFilterConfig,
  LastReportFilterConfig,
  ShowValueOnBarConfig
];
