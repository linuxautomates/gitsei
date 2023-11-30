import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { TestrailsCommonFiltersConfig } from "dashboard/report-filters/testrails/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { TESTRAILS_ACROSS_OPTIONS } from "../commonTestRailsReports.constants";
import { StackFilterConfig } from "./specific-filter.config.constants";

export const TestrailsTestEstimateReportFiltersConfig: LevelOpsFilter[] = [
  ...TestrailsCommonFiltersConfig,
  StackFilterConfig,
  ShowValueOnBarConfig,
  generateAcrossFilterConfig(TESTRAILS_ACROSS_OPTIONS),
  MaxRecordsFilterConfig
];
