import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMPrsFirstReviewMergeTrendReportType extends BaseSCMReportTypes {
  transform: string;
  filters_not_supporting_partial_filter: Array<string>;
}
