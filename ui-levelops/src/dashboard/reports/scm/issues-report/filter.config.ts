import { scmIssueHygieneTypes } from "dashboard/constants/hygiene.constants";
import { OUFiltersMapping } from "dashboard/constants/ou-filters";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { githubIssuesCommonFiltersConfig } from "dashboard/report-filters/github/github-issues-common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "./constant";

export const IssuesReportFiltersConfig: LevelOpsFilter[] = [
  ...githubIssuesCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS),
  IssueCreatedAtFilterConfig,
  ShowValueOnBarConfig,
  generateHygieneFilterConfig(scmIssueHygieneTypes.map((item: string) => ({ label: item, value: item }))),
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
