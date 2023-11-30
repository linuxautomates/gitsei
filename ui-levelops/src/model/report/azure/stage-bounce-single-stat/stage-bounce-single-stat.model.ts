import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";

export interface AzureStageBounceSingleStatReportType extends BaseAzureReportTypes {
  valuesToFilters: any;
  requiredFilters: string[];
  [WIDGET_VALIDATION_FUNCTION]: Function;
  compareField: string;
  supported_widget_types: string[];
  chart_click_enable: boolean;
}
