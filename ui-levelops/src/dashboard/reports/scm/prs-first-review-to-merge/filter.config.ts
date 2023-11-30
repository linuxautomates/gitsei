import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { AcrossFilterConfig } from "../prs-first-review-trend/specific-filter-config.constant";
import { PrClosedTimeFilterConfig, PrCreatedInFilterConfig } from "../scm-specific-filter-config.constant";

export const PrsFirstReviewToMergeTrendsFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig,
  AcrossFilterConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  MaxRecordsFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
