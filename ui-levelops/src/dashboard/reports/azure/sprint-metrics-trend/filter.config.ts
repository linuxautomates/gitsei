import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig, MetricViewByFilterConfig } from "../azure-specific-filter-config.constant";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import { generateParentStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { SprintGracePeriodFilterConfig } from "dashboard/report-filters/jira/SprintGracePeriod.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { generateMetricFilterConfig } from "./../../../report-filters/common/metrics-filter.config";
import { ACROSS_OPTIONS, METRICS_OPTIONS } from "./constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const SprintMetricTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  generateParentStoryPointsFilterConfig("workitem_parent_story_points", "Workitem Parent Story Points"),
  AzureIterationFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  LastNSprintsFilterConfig,
  MetricViewByFilterConfig,
  generateMetricFilterConfig(
    METRICS_OPTIONS,
    "multiple",
    ["creep_done_points", "commit_done_points", "commit_not_done_points", "creep_not_done_points"],
    "metric"
  ),
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  SprintGracePeriodFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
