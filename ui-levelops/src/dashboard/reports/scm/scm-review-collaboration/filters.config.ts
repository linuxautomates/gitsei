import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubPrsCommonFiltersConfig } from "dashboard/report-filters/github/github-prs-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  PrClosedTimeFilterConfig,
  PrCreatedInFilterConfig,
  PrMergedAtFilterConfig
} from "../scm-specific-filter-config.constant";
import { PrFilterConfig } from "./specific-filters-config.constant";

export const SCMReviewCollabReportFiltersConfig: LevelOpsFilter[] = [
  ...githubPrsCommonFiltersConfig,
  PrCreatedInFilterConfig,
  PrClosedTimeFilterConfig,
  PrMergedAtFilterConfig,
  PrFilterConfig,
  generateOUFilterConfig(
    {
      github: {
        options: OUFiltersMapping.github
      }
    },
    ["committer"]
  )
];
