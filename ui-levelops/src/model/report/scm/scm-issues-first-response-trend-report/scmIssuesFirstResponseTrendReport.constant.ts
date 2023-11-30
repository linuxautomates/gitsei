import { FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS, IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMIssuesFirstResponseTrendReportType extends BaseSCMReportTypes {
  transform: string;
  [IS_FRONTEND_REPORT]: boolean;
  [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: Array<string>;
}
