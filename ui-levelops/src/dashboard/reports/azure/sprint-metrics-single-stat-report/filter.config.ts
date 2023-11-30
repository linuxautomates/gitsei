import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { LastNSprintsFilterConfig } from "dashboard/report-filters/common/last-n-sprints-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
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
import { METRICS_OPTIONS } from "./constant";
import { IdealRangeFilterConfig } from "./specific-filter-config.constant";
import { PlannedSprintEndDateFilterConfig } from "dashboard/report-filters/common/planned-sprint-end-date-filter.config";
import { SprintStartDateFilterConfig } from "dashboard/report-filters/common/sprint-start-date-filter.config";

export const SprintMetricsSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  SprintEndDateFilterConfig,
  PlannedSprintEndDateFilterConfig,
  SprintStartDateFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  generateParentStoryPointsFilterConfig("workitem_parent_story_points", "Workitem Parent Story Points"),
  AzureIterationFilterConfig,
  LastNSprintsFilterConfig,
  generateMetricFilterConfig(METRICS_OPTIONS, "default", "avg_commit_to_done", "metric", "Metrics"),
  SprintGracePeriodFilterConfig,
  IdealRangeFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
