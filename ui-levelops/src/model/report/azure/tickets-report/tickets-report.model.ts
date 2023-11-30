import { CHART_DATA_TRANSFORMERS, MULTI_SERIES_REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { optionType } from "dashboard/dashboard-types/common-types";
import { METRIC_URI_MAPPING } from "dashboard/reports/azure/issues-report/constant";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureTicketsReportTypes extends BaseAzureReportTypes {
  appendAcrossOptions: Array<optionType>;
  stack_filters: string[];
  storyPointUri: string;
  infoMessages: { [x: string]: string };
  allow_key_for_stacks: boolean;
  weekStartsOnMonday: boolean;
  valuesToFilters: { [x: string]: string };
  filter_key_mapping: { [x: string]: string };
  prev_report_transformer: (args: any) => any;
  getTotalLabel: (data: { unit: string }) => "Total sum of story points" | "Total number of tickets" | "Total effort";
  sortApiDataHandler: (args: any) => any;
  xAxisLabelTransform: (params: any) => any;
  shouldReverseApiData: (params: any) => boolean;
  onChartClickPayload: (params: { [key: string]: any }) => any;
  getStacksStatus: (args: any) => boolean;
  [CHART_DATA_TRANSFORMERS]: any;
  [MULTI_SERIES_REPORT_FILTERS_CONFIG]: any;
  [METRIC_URI_MAPPING]?: Record<string, string>;
  onUnmountClearData?: boolean;
  acrossFilterLabelMapping: any;
  getDrillDownType: (params: any) => string;
  maxStackEntries: number;
  generateBarColors: (dataKey: string) => string;
}
