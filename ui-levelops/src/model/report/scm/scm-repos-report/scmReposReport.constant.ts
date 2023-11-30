import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMReposReportType extends BaseSCMReportTypes {
  filters_not_supporting_partial_filter: Array<string>;
}
