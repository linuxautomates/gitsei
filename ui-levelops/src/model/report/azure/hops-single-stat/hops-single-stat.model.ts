import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "../../../../dashboard/constants/filter-name.mapping";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureHopsSingleStatReportType extends BaseAzureReportTypes {
  valuesToFilters: Record<string, any>;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  compareField: string;
  supported_widget_types: string[];
  chart_click_enable: boolean;
}
