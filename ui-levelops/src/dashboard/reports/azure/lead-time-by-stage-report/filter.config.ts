import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  WorkitemresolvedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  azureCommonFiltersConfig,
  generateApplyFilterToNodeFilterConfig
} from "../azure-specific-filter-config.constant";
import { METRIC_OPTIONS } from "./constant";

export const LeadTimeByStageReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "mean", "metrics", "Stage Duration", true),
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  generateApplyFilterToNodeFilterConfig({ defaultValue: false }),
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
