import pagerdutyAckTrendReport from "./ack-trend/report";
import pagerdutyAfterHoursReport from "./after-hours/report";
import pagerdutyAlertReport from "./alert-report/report";
import pagerdutyIncidentReportTrends from "./incident-report-trends/report";
import pagerdutyIncidentReport from "./incident-report/report";
import pagerdutyReleaseIncidentReport from "./release-incidents/report";
import pagerdutyResponseTimeReport from "./response-time-report/report";
import pagerdutyStacksReport from "./stacks-report/report";
import pagerdutyTimeToResolve from "./time-to-resolve/report";

const pagerdutyReports = {
  ...pagerdutyIncidentReport,
  ...pagerdutyIncidentReportTrends,
  ...pagerdutyAfterHoursReport,
  ...pagerdutyAckTrendReport,
  ...pagerdutyReleaseIncidentReport,
  ...pagerdutyStacksReport,
  ...pagerdutyResponseTimeReport,
  ...pagerdutyAlertReport,
  ...pagerdutyTimeToResolve
};

export default pagerdutyReports;
