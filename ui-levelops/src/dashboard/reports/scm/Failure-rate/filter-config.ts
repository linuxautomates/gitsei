import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  PrClosedTimeFilterConfig,
  PrCreatedInFilterConfig,
  PrMergedAtFilterConfig,
  PrUpdatedAtFilterConfig
} from "../scm-specific-filter-config.constant";
import { scmDoraCommonFiltersConfig } from "dashboard/report-filters/github/github-dora-common-filters.config";
import { LeadTimeConfigurationProfileFilterConfig } from "dashboard/report-filters/common/lead-time-configuration-profile-filter.config";

export const SCMDoraFailureRateFilterConfig: LevelOpsFilter[] = [
  ...scmDoraCommonFiltersConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  PrMergedAtFilterConfig,
  PrUpdatedAtFilterConfig,
  LeadTimeConfigurationProfileFilterConfig
];
