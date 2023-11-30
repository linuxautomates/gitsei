import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { bullseyeAcrossOptions } from "./constants";
import { bullseyeCommonFiltersConfig } from "../bullseye-specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const CodeCoverageReportFiltersConfig: LevelOpsFilter[] = [
  ...bullseyeCommonFiltersConfig,
  generateAcrossFilterConfig(bullseyeAcrossOptions),
  MaxRecordsFilterConfig,
  ShowValueOnBarConfig
];
