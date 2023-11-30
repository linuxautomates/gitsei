import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { CicdJobEndDateFilterConfig } from "dashboard/report-filters/common/cicd-job-end-date-filter.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { StageDurationFilterConfig } from "dashboard/report-filters/common/stage-duration-filter.config";
import { githubPrLeadTimeCommonFiltersConfig } from "dashboard/report-filters/github/github-leadtime-common-filters.configs";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  PrCreatedInFilterConfig,
  PrMergedAtFilterConfig,
  CommittedInFilterConfig,
  PrClosedTimeFilterConfig
} from "../scm-specific-filter-config.constant";
import { ApplyFilterToNodeFilterConfig } from "dashboard/reports/azure/azure-specific-filter-config.constant";

export const PrLeadTimeByStageReportFiltersConfig: LevelOpsFilter[] = [
  ...githubPrLeadTimeCommonFiltersConfig.filter((item: LevelOpsFilter) => item.beKey !== "branches"),
  StageDurationFilterConfig,
  CommittedInFilterConfig,
  ApplyFilterToNodeFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  PrMergedAtFilterConfig,
  CicdJobEndDateFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
