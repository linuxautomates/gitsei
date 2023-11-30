import issuesSingleStatReport from "./issues-single-stat/report";
import issuesReport from "./issues-report/report";
import backlogTrendReport from "./backlog-trend-report/report";
import issuesTrendReport from "./issues-report-trend/report";
import bounceReport from "./bounce-report/report";
import bounceTrendReport from "./bounce-report-trends/report";
import bounceSingleStatReport from "./bounce-single-stat/report";
import firstAssigneeReport from "./first-assignee-report/report";
import hopsReport from "./hops-report/report";
import hopsTrendReport from "./hops-report-trends/report";
import hopsSingleStatReport from "./hops-single-stat/report";
import hygieneReport from "./hygiene-report/report";
import hygieneReportTrends from "./hygiene-report-trends/report";
import effortAlignmentReport from "./effort-alignment-report/report";
import effortInvestmentEngineerReport from "./effort-investment-engineer-report/report";
import effortInvestmentSingleStatReport from "./effort-investment-single-stat/report";
import effortInvestmentTrendReport from "./effort-investment-trend-report/report";
import leadTimeByStageReport from "./lead-time-by-stage-report/report";
import leadTimeByTypeReport from "./lead-time-by-type-report/report";
import leadTimeSingleStatReport from "./lead-time-single-stat/report";
import leadTimeTrendReport from "./lead-time-trend-report/report";
import resolutionTimeReport from "./resolution-time/report";
import resolutionTimeSingleStatReport from "./resolution-time-single-stat/report";
import resolutionTimeTrendReport from "./resolution-time-trend/report";
import responseTimeReport from "./response-time-report/report";
import responseTimeSingleStat from "./response-time-single-stat/report";
import responseTimeTrendReport from "./response-time-trend/report";
import sprintImpactUnestimatedTicketsReport from "./sprint-impact-of-unestimated-tickets-report/report";
import sprintMetricsPercentageTrendReport from "./sprint-metric-percentage-trend/report";
import sprintMetricsSingleStatReport from "./sprint-metrics-single-stat-report/report";
import sprintMetricsTrendReport from "./sprint-metrics-trend/report";
import stageBounceReport from "./stage-bounce-report/report";
import stageBounceSingleStat from "./stage-bounce-single-stat/report";
import issueTimeAcrossStagesReport from "./time-across-stages/report";
import azureIssuesProgressReport from "./issues-progress-report/report";
import azureProgramProgressReport from "./program-progress-report/report";

const azureReports = {
  ...backlogTrendReport,
  ...bounceReport,
  ...bounceTrendReport,
  ...bounceSingleStatReport,
  ...effortAlignmentReport,
  ...effortInvestmentEngineerReport,
  ...effortInvestmentSingleStatReport,
  ...effortInvestmentTrendReport,
  ...firstAssigneeReport,
  ...hopsReport,
  ...hopsTrendReport,
  ...hopsSingleStatReport,
  ...hygieneReport,
  ...hygieneReportTrends,
  ...issuesReport,
  ...issuesTrendReport,
  ...issuesSingleStatReport,
  ...leadTimeByStageReport,
  ...leadTimeByTypeReport,
  ...leadTimeSingleStatReport,
  ...leadTimeTrendReport,
  ...resolutionTimeReport,
  ...resolutionTimeSingleStatReport,
  ...resolutionTimeTrendReport,
  ...responseTimeReport,
  ...responseTimeSingleStat,
  ...responseTimeTrendReport,
  ...sprintImpactUnestimatedTicketsReport,
  ...sprintMetricsPercentageTrendReport,
  ...sprintMetricsSingleStatReport,
  ...sprintMetricsTrendReport,
  ...stageBounceReport,
  ...stageBounceSingleStat,
  ...issueTimeAcrossStagesReport,
  ...azureIssuesProgressReport,
  ...azureProgramProgressReport
};

export default azureReports;
