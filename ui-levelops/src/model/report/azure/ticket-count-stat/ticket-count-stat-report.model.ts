import { METRIC_URI_MAPPING } from "dashboard/reports/azure/issues-single-stat/constant";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureTicketsCountSingleStat extends BaseAzureReportTypes {
  compareField: string;
  supported_widget_types: string[];
  chart_click_enable: boolean;
  default_metadata: any;
  prev_report_transformer: (data: any) => any;
  widget_validation_function?: (payload: any) => boolean;
  storyPointUri?: string;
  getTotalLabel?: (params: any) => void;
  [METRIC_URI_MAPPING]?: Record<string, string>;
  onUnmountClearData?: boolean;
}
