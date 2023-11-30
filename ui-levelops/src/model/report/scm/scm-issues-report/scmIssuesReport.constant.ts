import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMIssuesReportType extends BaseSCMReportTypes {
  filters_not_supporting_partial_filter: Array<string>;
}
