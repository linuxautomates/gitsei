import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMLeadTimeTrendReportType extends BaseSCMReportTypes {
  shouldJsonParseXAxis: () => boolean;
}
