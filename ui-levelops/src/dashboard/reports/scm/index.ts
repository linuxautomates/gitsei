import { scmCodingDaysSingleStat } from "./coding-days-single-stat/report";
import { scmCodingDaysReport } from "./coding-days/report";
import { scmCommitsStatReport } from "./commits-single-stat/report";
import { SCMCommitsReport } from "./commits/report";
import { scmCommittersReport } from "./committers/report";
import deploymentFrequencySingleStat from "./deployment-frequency-single-stat/report";
import scmDoraFailureRateSingleStat from "./Failure-rate/report";
import { scmFilesReport } from "./files-report/report";
import { scmIssuesResolutionTimeReport } from "./issue-resolution-time/report";
import { scmIssuesCountSingleStatReport } from "./issues-count-single-stat/report";
import { scmIssuesFirstResponseTrendReport } from "./issues-first-response-trend/report";
import { scmIssuesFirstResponseSingleStatReport } from "./issues-first-response-single-stat/report";
import { scmIssuesFirstResponseReport } from "./issues-first-response/report";
import { scmIssuesReportTrends } from "./issues-report-trends/report";
import { scmIssuesReport } from "./issues-report/report";
import { scmIssuesTimeAcrossStages } from "./issues-time-across-stages/report";
import leadTimeForChangesSingleStat from "./lead-time-for-changes-single-stat/report";
import { scmPrsFirstReviewToMergeTrendSingleStat } from "./pr-first-review-to-merge-trend-single-stat/report";
import { scmPrsFirstReviewTrendSingleStat } from "./pr-first-review-trend-single-stat/report";
import { scmPrLeadTimeByStageReport } from "./pr-lead-time-by-stage/report";
import { scmPrLeadTimeTrendReport } from "./pr-lead-time-trend/report";
import { SCMPrsReport } from "./pr-report/report";
import { scmPrsResponseTimeReport } from "./pr-response-time/report";
import { scmFirstReviewToMergeTrend } from "./prs-first-review-to-merge/report";
import { scmPrsFirstReviewTrendReport } from "./prs-first-review-trend/report";
import { scmPrsMergeTrendSingleStat } from "./prs-merge-trend-single-stat/report";
import { scmPrsMergeTrendReport } from "./prs-merge-trend/report";
import { scmPrsResponseTimeSingleStat } from "./prs-response-time-single-stat/report";
import { scmPRsSingleStatReport } from "./prs-single-stat/report";
import { scmReposReport } from "./repos/report";
import { scmReviewCollaborationReport } from "./review-collaboration-report/report";
import { scmReworkReport } from "./rework-report/report";
import { scmFilesTypesReport } from "./scm-file-types-report/report";
import { scmJiraFilesReport } from "./scm-jira-files-report/report";
import scmDoraTimeToRecoverSingleStat from "./time-to-recover/report";
import { GithubDashboards } from "dashboard/constants/applications/github.application";

const scmReports = {
  ...leadTimeForChangesSingleStat,
  ...deploymentFrequencySingleStat,
  ...scmDoraTimeToRecoverSingleStat,
  ...scmDoraFailureRateSingleStat,
  ...SCMPrsReport,
  ...SCMCommitsReport,
  ...scmPRsSingleStatReport,
  ...scmCommitsStatReport,
  ...scmPrsMergeTrendReport,
  ...scmPrsFirstReviewTrendReport,
  ...scmFirstReviewToMergeTrend,
  ...scmFilesReport,
  ...scmIssuesReport,
  ...scmIssuesReportTrends,
  ...scmIssuesCountSingleStatReport,
  ...scmIssuesFirstResponseReport,
  ...scmIssuesFirstResponseTrendReport,
  ...scmIssuesFirstResponseSingleStatReport,
  ...scmPrsMergeTrendSingleStat,
  ...scmPrsFirstReviewTrendSingleStat,
  ...scmPrsFirstReviewToMergeTrendSingleStat,
  ...scmJiraFilesReport,
  ...scmReposReport,
  ...scmCommittersReport,
  ...scmFilesTypesReport,
  ...scmIssuesResolutionTimeReport,
  ...scmPrLeadTimeTrendReport,
  ...scmPrLeadTimeByStageReport,
  ...scmIssuesTimeAcrossStages,
  ...scmCodingDaysReport,
  ...scmCodingDaysSingleStat,
  ...scmPrsResponseTimeReport,
  ...scmPrsResponseTimeSingleStat,
  ...scmReworkReport,
  ...scmReviewCollaborationReport,
  lead_time_single_stat: GithubDashboards.lead_time_single_stat // temp fix. Will remove once refactoring of this report is done.
};

export default scmReports;
