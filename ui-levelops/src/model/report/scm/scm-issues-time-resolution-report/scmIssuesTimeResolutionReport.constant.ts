import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMIssuesTimeResolutionReportType extends BaseSCMReportTypes {
  tooltipMapping: any;
  weekStartsOnMonday: boolean;
  dataKey: Array<string>;
  get_graph_filters: any;
}
