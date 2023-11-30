import {
  coverityCommonFiltersConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig
} from "../coverity-specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssuesTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...coverityCommonFiltersConfig,
  FirstDetectedAtFilterConfig,
  LastDetectedAtFilterConfig
];
