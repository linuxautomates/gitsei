import { PagerdutyServicesCommonFiltersConfig } from "dashboard/report-filters/pagerduty/pagerduty-services-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { OfficeHoursFilterConfig } from "../time-to-resolve/specific-filters-config.constant";
export const PagerdutyAfterHoursReportFiltersConfig: LevelOpsFilter[] = [
  ...PagerdutyServicesCommonFiltersConfig,
  OfficeHoursFilterConfig
];
