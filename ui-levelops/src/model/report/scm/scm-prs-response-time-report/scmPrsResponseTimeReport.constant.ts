import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMPrsResponseTimeReportType extends BaseSCMReportTypes {
  filters_not_supporting_partial_filter: Array<string>;
  reviewerUri: string;
  weekStartsOnMonday: boolean;
}
