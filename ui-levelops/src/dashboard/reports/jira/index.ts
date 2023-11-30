import jiraResolutionTimeReport from "./resolution-time-report/report";
import resolutionTimeReportTrends from "./resolution-time-report-trends/report";
import jiraResponseTimeReport from "./response-time-report/report";
import jiraResponseTimeReportTrends from "./response-time-report-trends/report";
import sprintGoalReport from "./sprint-goal-report/report";
import jiraBounceReport from "./bounce-report/report";
import jiraBounceReportTrends from "./bounce-reports-trends/report";
import issuesFirstAssigneeReport from "./first-assignee-report/report";
import jiraHopsReport from "./jira-hops-report/report";
import issueAssigneeTimeReport from "./issue-assignee-time-report/report";
import issuesReport from "./issues-report/report";
import jiraBounceSingleStatReport from "./bounce-report-single-stat/report";
import jiraHopsCountStat from "./hops-count-stat/report";
import jiraHopsReportTrends from "./hops-trend-report/report";
import resolutionTimeCountStat from "./resolution-time-count-stat/report";
import responseTimeSingleStat from "./response-time-single-stat/report";
import sprintMetricSingleStat from "./sprint-metric-single-stat/reports";
import sprintMetricPercentageTrendReport from "./sprint-metric-percentage-trend-report/reports";
import sprintMetricTrendReport from "./sprint-metric-trend-report/report";
import sprintImpactOfUnestimatedTicketsReport from "./sprint-impact-of-unestimated-tickets-report/report";
import hygieneReportsTrend from "./hygiene-report-trends/report";
import hygieneReport from "./hygiene-report/report";
import ticketsReportTrend from "./tickets-report-trend/report";
import ticketsCountsStat from "./tickets-counts-stat/report";
import jiraTicketsCountByFirstAssigneeReport from "./jira-tickets-count-by-first-assignee/report";
import jiraBacklogTrendReport from "./jira-backlog-trend-report/report";
import jiraTimeAcrossStagesReport from "./jira-time-across-stages/report";
import leadTimeTrendReport from "./lead-time-trend-report/report";
import leadTimeByStageReport from "./lead-time-by-stage-report/report";
import leadTimeByTypeReport from "./lead-time-by-type-report/report";
import sprintDistributionRetrospectiveReport from "./sprint-distribution-retrospective-report/report";
import stageBounceReport from "./stage-bounce-report/report";
import stageBounceSingleStat from "./stage-bounce-single-stat/report";
import leadTimeByTimeSpentInStagesReport from "./lead-time-by-time-spent-in-stages/report";
import jiraReleaseTableReport from "./jira-release-table-report/report";

const jiraReports = {
  ...jiraResolutionTimeReport,
  ...resolutionTimeReportTrends,
  ...jiraResponseTimeReport,
  ...jiraResponseTimeReportTrends,
  ...sprintGoalReport,
  ...jiraBounceReport,
  ...jiraBounceReportTrends,
  ...issuesFirstAssigneeReport,
  ...jiraHopsReport,
  ...issueAssigneeTimeReport,
  ...issuesReport,
  ...jiraBounceSingleStatReport,
  ...jiraHopsCountStat,
  ...jiraHopsReportTrends,
  ...resolutionTimeCountStat,
  ...responseTimeSingleStat,
  ...sprintMetricSingleStat,
  ...sprintMetricPercentageTrendReport,
  ...sprintMetricTrendReport,
  ...sprintImpactOfUnestimatedTicketsReport,
  ...hygieneReportsTrend,
  ...hygieneReport,
  ...ticketsReportTrend,
  ...ticketsCountsStat,
  ...jiraBacklogTrendReport,
  ...jiraTicketsCountByFirstAssigneeReport,
  ...jiraTimeAcrossStagesReport,
  ...leadTimeTrendReport,
  ...leadTimeByStageReport,
  ...leadTimeByTypeReport,
  ...sprintDistributionRetrospectiveReport,
  ...stageBounceReport,
  ...stageBounceSingleStat,
  ...leadTimeByTimeSpentInStagesReport,
  ...jiraReleaseTableReport
};

export default jiraReports;
