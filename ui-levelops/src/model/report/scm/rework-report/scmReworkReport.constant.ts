import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMReworkReportType extends BaseSCMReportTypes {
  [PREV_REPORT_TRANSFORMER]: any;
}
