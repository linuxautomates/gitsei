import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { StageDurationFilterConfig } from "dashboard/report-filters/common/stage-duration-filter.config";
import { generateStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  ApplyFilterToNodeFilterConfig,
  AzureCodeAreaFilterConfig,
  azureCommonFiltersConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { CALCULATION_OPTIONS } from "./constant";
import { GithubFiltersConfig } from "./specific-filter.config";

export const LeadTimeSingleStatReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  ...githubPrsCommonFiltersConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  generateMetricFilterConfig(CALCULATION_OPTIONS, "default", "ticket_velocity", "calculation"),
  generateStoryPointsFilterConfig("workitem_story_points", "AZURE STORY POINTS"),
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  StageDurationFilterConfig,
  ApplyFilterToNodeFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  }),
  GithubFiltersConfig
];
