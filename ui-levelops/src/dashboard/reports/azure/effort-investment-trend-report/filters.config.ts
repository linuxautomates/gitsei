import {
  azureActiveWorkUnitOptions,
  azureEffortInvestmentUnitFilterOptions
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { azureCommonFiltersConfig } from "../azure-specific-filter-config.constant";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubCommitsCommonFiltersConfig } from "dashboard/report-filters/github/github-commits-common-filters.config";
import { generateActiveWorkUnitFilter } from "dashboard/report-filters/jira/active-work-unit-filter.config";
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
import { sampleIntervalFiltersConfig } from "./specific-filters-config.constant";

export const AzureEITrendReportFiltersConfig: LevelOpsFilter[] = [
  ...azureCommonFiltersConfig.filter(
    (item: LevelOpsFilter) => !["workitem_statuses", "workitem_status_categories"].includes(item.beKey)
  ),
  WorkitemTicketCategoryFilterConfig,
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig,
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  WorkitemStoryPointsConfig,
  IssueManagementSystemFilterConfig,
  generateEffortUnitFilter({
    defaultValue: "azure_effort_investment_tickets",
    effortUnitOptions: azureEffortInvestmentUnitFilterOptions
  }),
  generateActiveWorkUnitFilter({
    defaultValue: "active_azure_ei_ticket_count",
    activeWorkUnitOptions: azureActiveWorkUnitOptions
  }),
  WorkitemCreatedAtFilterConfig,
  WorkitemUpdatedAtFilterConfig,
  {
    ...WorkitemresolvedAtFilterConfig,
    required: true,
    deleteSupport: false
  },
  sampleIntervalFiltersConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];

export const AzureCommitEITrendReportFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFiltersConfig,
  {
    ...CommittedInFilterConfig,
    required: true,
    deleteSupport: false
  },
  { ...generateEffortInvestmentProfileFilter({ withProfileCategory: false }), required: true },
  IssueManagementSystemFilterConfig,
  generateEffortUnitFilter({
    defaultValue: "azure_effort_investment_tickets",
    effortUnitOptions: azureEffortInvestmentUnitFilterOptions
  }),
  generateActiveWorkUnitFilter({
    defaultValue: "active_azure_ei_ticket_count",
    activeWorkUnitOptions: azureActiveWorkUnitOptions
  }),
  sampleIntervalFiltersConfig,
  generateOUFilterConfig({
    azure_devops: {
      options: OUFiltersMapping.azure_devops
    }
  })
];
