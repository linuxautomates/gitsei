import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMCommittersReportType extends BaseSCMReportTypes {
  filters_not_supporting_partial_filter: Array<string>;
}
