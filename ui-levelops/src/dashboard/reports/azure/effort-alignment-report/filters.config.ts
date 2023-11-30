import { azureActiveWorkUnitOptions } from "dashboard/constants/bussiness-alignment-applications/constants";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { generateActiveWorkUnitFilter } from "dashboard/report-filters/jira/active-work-unit-filter.config";
import { generateEffortInvestmentProfileFilter } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemTicketCategoryFilterConfig
} from "../azure-specific-filter-config.constant";

export const AzureEffortAlignmentReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig.filter(
    (item: LevelOpsFilter) => !["workitem_statuses", "workitem_status_categories"].includes(item.beKey)
  ),
  WorkitemTicketCategoryFilterConfig,
  {
    ...WorkitemresolvedAtFilterConfig,
    required: true,
    deleteSupport: false
  },
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  generateActiveWorkUnitFilter({
    defaultValue: "active_azure_ei_ticket_count",
    activeWorkUnitOptions: azureActiveWorkUnitOptions
  }),
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
