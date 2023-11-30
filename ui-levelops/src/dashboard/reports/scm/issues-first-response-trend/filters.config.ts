import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { githubIssuesCommonFiltersConfig } from "dashboard/report-filters/github/github-issues-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constant";

export const IssuesFirstResponseTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...githubIssuesCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  IssueCreatedAtFilterConfig,
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
