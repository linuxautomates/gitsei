import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { HIDE_REPORT } from "dashboard/constants/filter-key.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureSprintImpactUnestimatedTicketReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [HIDE_REPORT]: boolean;
}
