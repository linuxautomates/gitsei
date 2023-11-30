import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureBounceSingleStatReportType extends BaseAzureReportTypes {
  valuesToFilters: Record<string, any>;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  compareField: string;
  supported_widget_types: any;
  xAxisLabelTransform: (param: any) => string;
  chart_click_enable: boolean;
}
