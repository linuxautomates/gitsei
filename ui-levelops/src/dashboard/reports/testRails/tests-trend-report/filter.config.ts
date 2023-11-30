import { TestrailsCommonFiltersConfig } from "dashboard/report-filters/testrails/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { MetricFilterConfig } from "../tests-report/specific-filter.config.constants";

export const TestrailsTestTrendsReportFiltersConfig: LevelOpsFilter[] = [
    ...TestrailsCommonFiltersConfig,
    MetricFilterConfig
];
