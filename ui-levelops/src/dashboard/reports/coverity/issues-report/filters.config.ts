import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import {
  coverityCommonFiltersConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig
} from "../coverity-specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const IssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...coverityCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  MaxRecordsFilterConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig,
  ShowValueOnBarConfig
];
