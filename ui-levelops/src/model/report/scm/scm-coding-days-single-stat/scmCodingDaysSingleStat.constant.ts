import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMCodingDaysSingleStatReportType extends BaseSCMReportTypes {
  compareField: string;
  widget_validation_function: (query: any) => boolean;
  show_in_days: boolean;
  [PREV_REPORT_TRANSFORMER]: (widget: any) => void;
}
