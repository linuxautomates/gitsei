import { pagerdutyTimeToResolveMetricsOptions } from "dashboard/graph-filters/components/Constants";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateIssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { PagerdutyCommonFiltersConfig } from "dashboard/report-filters/pagerduty/pagerduty-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constants";
import {
  IssueTypeFilterConfig,
  LeftYAxisFilterConfig,
  OfficeHoursFilterConfig,
  SampleIntervalFilterConfig,
  StackFilterConfig
} from "./specific-filters-config.constant";

export const PagerDutyTimeToResolveFiltersConfig: LevelOpsFilter[] = [
  ...PagerdutyCommonFiltersConfig,
  OfficeHoursFilterConfig,
  SampleIntervalFilterConfig,
  SortXAxisFilterConfig,
  StackFilterConfig,
  LeftYAxisFilterConfig,
  IssueTypeFilterConfig,
  generateIssueCreatedAtFilterConfig([], "Incident Created At", "incident_created_at"),
  generateIssueCreatedAtFilterConfig([], "Incident Resolved At", "incident_resolved_at"),
  generateIssueCreatedAtFilterConfig([], "Alert Created At", "alert_created_at"),
  generateIssueCreatedAtFilterConfig([], "Alert Resolved At", "alert_resolved_at"),
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateMetricFilterConfig(pagerdutyTimeToResolveMetricsOptions, "default", "resolve", "metric")
];
