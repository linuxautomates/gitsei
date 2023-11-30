import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { PagerdutyServicesCommonFiltersConfig } from "dashboard/report-filters/pagerduty/pagerduty-services-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";
import { StackFilterConfig } from "./specific-filter.config.constants";

export const PagerdutyStacksReportFiltersConfig: LevelOpsFilter[] = [
  ...PagerdutyServicesCommonFiltersConfig,
  StackFilterConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS)
];
