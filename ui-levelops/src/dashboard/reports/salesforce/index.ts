import salesforceBounceReport from "./bounce-report/report";
import salesforceHopsReport from "./hops-report/report";
import salesforceHygieneReport from "./hygiene-report/report";
import salesforceResolutionTimeReport from "./resolution-time-report/report";
import salesforceTicketsTrendReport from "./ticket-trends-report/report";
import salesforceTicketsReport from "./tickets-report/report";
import salesforceTopCustomersReport from "./top-customers-report/report";

const salesforceReports = {
  ...salesforceBounceReport,
  ...salesforceHopsReport,
  ...salesforceResolutionTimeReport,
  ...salesforceTicketsReport,
  ...salesforceHygieneReport,
  ...salesforceTicketsTrendReport,
  ...salesforceTopCustomersReport
};

export default salesforceReports;
