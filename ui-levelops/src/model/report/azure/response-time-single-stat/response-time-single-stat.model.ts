import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureResponseTimeSingleStatReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  compareField: string;
  supported_widget_types: string[];
  chart_click_enable: boolean;
}
