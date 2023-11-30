import coverityIssuesReport from "./issues-report/report";
import coverityIssuesStatReport from "./issues-stat-report/report";
import coverityIssuesTrendReport from "./issues-trend-report/report";

const coverityReports = {
  ...coverityIssuesReport,
  ...coverityIssuesTrendReport,
  ...coverityIssuesStatReport
};

export default coverityReports;
