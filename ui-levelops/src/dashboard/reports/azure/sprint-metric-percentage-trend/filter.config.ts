import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig, MetricViewByFilterConfig } from "../azure-specific-filter-config.constant";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateParentStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
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
import { LastNSprintsFilterConfig } from "./../../../report-filters/common/last-n-sprints-filter.config";
import { SprintEndDateFilterConfig } from "./../../../report-filters/common/sprint-end-date-filter.config";
import { ACROSS_OPTIONS, METRICS_OPTIONS, VISUALIZATION_OPTIONS } from "./constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const SprintMetricPercentageTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  generateParentStoryPointsFilterConfig("workitem_parent_story_points", "Workitem Parent Story Points"),
  AzureIterationFilterConfig,
  {
    ...generateVisualizationFilterConfig(VISUALIZATION_OPTIONS),
    defaultValue: "stacked_area"
  },
  SprintGracePeriodFilterConfig,
  IssueManagementSystemFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  LastNSprintsFilterConfig,
  MetricViewByFilterConfig,
  generateMetricFilterConfig(
    METRICS_OPTIONS,
    "multiple",
    ["commit_done_ratio", "creep_done_to_commit_ratio"],
    "metric"
  ),
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
