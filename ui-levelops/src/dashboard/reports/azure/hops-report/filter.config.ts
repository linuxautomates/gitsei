import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig
} from "../azure-specific-filter-config.constant";
import { AcrossFilterConfig } from "./specific-filter-config.constant";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";

export const IssueHopsReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  AcrossFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  SortXAxisFilterConfig,
  MaxRecordsFilterConfig,
  ShowValueOnBarConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
