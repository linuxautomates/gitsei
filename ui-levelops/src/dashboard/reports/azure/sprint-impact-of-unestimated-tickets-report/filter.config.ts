import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig, MetricViewByFilterConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SprintEndDateFilterConfig } from "dashboard/report-filters/common/sprint-end-date-filter.config";
import {
  generateParentStoryPointsFilterConfig,
  generateStoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { SampleIntervalFilterConfig } from "./specific-filter-config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { METRIC_OPTIONS } from "./constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const SprintImpactOfUnestimatedTicketReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  generateParentStoryPointsFilterConfig("workitem_parent_story_points", "Workitem Parent Story Points"),
  MetricViewByFilterConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", undefined, "metric"),
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  LastNSprintsFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  SampleIntervalFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
