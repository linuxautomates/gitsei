import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import {
  azureCommonFiltersConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemUserStoryFilterConfig
} from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { HygieneWeightsFiltersConfig } from "dashboard/report-filters/jira/hygiene-weights-filters-config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig
} from "../azure-specific-filter-config.constant";
import {
  HideScoreFilterConfig,
  IdleLengthFilterConfig,
  PoorDescriptionFilterConfig
} from "./specific-filter-config.constant";

export const IssueHygieneReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  HygieneWeightsFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemFeaturesFilterConfig,
  WorkitemUserStoryFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  PoorDescriptionFilterConfig,
  IdleLengthFilterConfig,
  HideScoreFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
