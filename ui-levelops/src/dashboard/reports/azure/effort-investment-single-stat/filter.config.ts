import { azureEffortInvestmentUnitFilterOptions } from "dashboard/constants/bussiness-alignment-applications/constants";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { generateEffortInvestmentProfileFilter } from "dashboard/report-filters/jira/effort-investment-profile.filter";
import { generateEffortUnitFilter } from "dashboard/report-filters/jira/effort-unit-filter.config";
import { CommittedInFilterConfig } from "dashboard/reports/scm/scm-specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  AzureTeamsFilterConfig,
  WorkitemCreatedAtFilterConfig,
  WorkitemresolvedAtFilterConfig,
  WorkitemStoryPointsConfig,
  WorkitemUpdatedAtFilterConfig,
  WorkitemTicketCategoryFilterConfig
} from "../azure-specific-filter-config.constant";

export const AzureEffortInvestmentSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig.filter(
    (item: LevelOpsFilter) => !["workitem_statuses", "workitem_status_categories"].includes(item.beKey)
  ),
  WorkitemTicketCategoryFilterConfig,
  {
    ...WorkitemresolvedAtFilterConfig,
    required: true,
    deleteSupport: false
  },
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  generateEffortUnitFilter({
    defaultValue: "azure_effort_investment_tickets",
    effortUnitOptions: azureEffortInvestmentUnitFilterOptions
  }),
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];

export const AzureCommitEffortInvestmentSingleStatFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  {
    ...CommittedInFilterConfig,
    required: true,
    deleteSupport: false
  },
  IssueManagementSystemFilterConfig,
  generateEffortUnitFilter({
    defaultValue: "azure_effort_investment_tickets",
    effortUnitOptions: azureEffortInvestmentUnitFilterOptions
  }),
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
