import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { SortXAxisFilterConfig } from "dashboard/report-filters/common/sort-xaxis-filter.config";
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
import { ACROSS_OPTIONS } from "./constant";
import { removeCustomPrefix } from "../common-helper";

export const FirstAssigneeReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
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
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  SortXAxisFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
