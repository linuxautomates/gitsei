import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  PrMergedAtFilterConfig,
  PrUpdatedAtFilterConfig
} from "./scm-specific-filter-config.constant";
import { scmDoraCommonFiltersConfig } from "dashboard/report-filters/github/github-dora-common-filters.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";

export const ScmCommonPRFilter: LevelOpsFilter[] = [
  ...scmDoraCommonFiltersConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  PrMergedAtFilterConfig,
  PrUpdatedAtFilterConfig,
  LeadTimeConfigurationProfileFilterConfig,
  generateOUFilterConfig({
    github: {
      options: OUFiltersMapping.github
    }
  })
];
