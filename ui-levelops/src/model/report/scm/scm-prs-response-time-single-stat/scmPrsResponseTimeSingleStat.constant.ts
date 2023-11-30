import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMPrsResponseTimeSingleStatReportType extends BaseSCMReportTypes {
  reviewerUri: string;
  compareField: string;
}
