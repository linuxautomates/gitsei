import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMIssuesFirstResponseReportType extends BaseSCMReportTypes {
  filters_not_supporting_partial_filter: Array<string>;
}
