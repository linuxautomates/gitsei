import devOrgUnitScoreReport from "./org-unit-score-report/report";
import devScoreProductivityReport from "./dev-productivity-score-report/report";
import devProductivityPRActivityReport from "./dev-productivity-pr-activity/report";
import devRawStatsReport from "./individual-raw-stats-report/report";
import orgRawStatsReport from "./org-raw-stats-report/report";

const DevProductivityReports = {
  ...devOrgUnitScoreReport,
  ...devScoreProductivityReport,
  ...devProductivityPRActivityReport,
  ...devRawStatsReport,
  ...orgRawStatsReport
};

export default DevProductivityReports;
