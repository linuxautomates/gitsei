import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureTicketsTrendReportType extends BaseAzureReportTypes {
  valuesToFilters: { [x: string]: string };
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
}
