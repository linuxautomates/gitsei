import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { get, uniqBy } from "lodash";
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
import { ACROSS_OPTIONS, METRIC_OPTIONS } from "./constant";
import { HideStatusFilterConfig } from "./specific-filter-config.constant";
import { removeCustomPrefix } from "../common-helper";

export const IssueTimeAcrossFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  { ...WorkitemresolvedAtFilterConfig, required: true, deleteSupport: false },
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  WorkitemStoryPointsConfig,
  AzureIterationFilterConfig,
  generateMetricFilterConfig(METRIC_OPTIONS, "default", "median_time", "metric"),
  generateAcrossFilterConfig((args: any) => {
    const commonOptions = ACROSS_OPTIONS;
    const filterMetaData = get(args, ["filterMetaData"], {});
    const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
    const customFieldsOptions = customFieldsData.map((item: any) => ({
      label: item.name,
      value: removeCustomPrefix(item)
    }));
    return uniqBy([...commonOptions, ...customFieldsOptions], "value");
  }),
  HideStatusFilterConfig,
  MaxRecordsFilterConfig,
  IssueManagementSystemFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
