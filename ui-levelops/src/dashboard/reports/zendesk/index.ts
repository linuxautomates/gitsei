import zendeskAgentWaitTimeTrendReport from "./agent-wait-time-report-trends/report";
import zendeskAgentWaitTimeReport from "./agent-wait-time-report/report";
import zendeskBounceTrendReport from "./bounce-report-trends/report";
import zendeskBounceReport from "./bounce-report/report";
import zendeskHopsTrendReport from "./hops-report-trends/report";
import zendeskHopsReport from "./hops-report/report";
import zendeskHygieneTrendReport from "./hygiene-report-trends/report";
import zendeskHygieneReport from "./hygiene-report/report";
import zendeskReopensTrendReport from "./reopens-report-trends/report";
import zendeskReopensReport from "./reopens-report/report";
import zendeskRepliesReportTrend from "./replies-report-trends/report";
import zendeskRepliesReport from "./replies-report/report";
import zendeskRequesterWaitTimeTrendReport from "./requester-wait-time-report-trends/report";
import zendeskRequesterWaitTimeReport from "./requester-wait-time-report/report";
import zendeskResolutionTimeTrendReport from "./resolution-time-report-trends/report";
import zendeskResolutionTimeReport from "./resolution-time-report/report";
import zendeskResponseTimeTrendReport from "./response-time-report-trends/report";
import zendeskResponseTimeReport from "./response-time-report/report";
import zendeskTicketsTrendReport from "./tickets-report-trends/report";
import zendeskTicketsReport from "./tickets-report/report";
import zendeskTopCustomerReport from "./top-customers-report/report";

const zendeskReports = {
  ...zendeskBounceReport,
  ...zendeskHopsReport,
  ...zendeskResponseTimeReport,
  ...zendeskResolutionTimeReport,
  ...zendeskTicketsReport,
  ...zendeskHygieneReport,
  ...zendeskReopensReport,
  ...zendeskRepliesReport,
  ...zendeskAgentWaitTimeReport,
  ...zendeskRequesterWaitTimeReport,
  ...zendeskBounceTrendReport,
  ...zendeskHopsTrendReport,
  ...zendeskResponseTimeTrendReport,
  ...zendeskResolutionTimeTrendReport,
  ...zendeskTicketsTrendReport,
  ...zendeskHygieneTrendReport,
  ...zendeskRequesterWaitTimeTrendReport,
  ...zendeskAgentWaitTimeTrendReport,
  ...zendeskRepliesReportTrend,
  ...zendeskReopensTrendReport,
  ...zendeskTopCustomerReport
};

export default zendeskReports;
