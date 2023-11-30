import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";

export interface AzureResponseTimeTrendReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  valuesToFilters: any;
}
