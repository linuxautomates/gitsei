import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMIssuesTimeAcrossStagesReportType extends BaseSCMReportTypes {
  dataKey: string;
  weekStartsOnMonday: boolean;
  getTotalKey: (params: any) => string;
}
