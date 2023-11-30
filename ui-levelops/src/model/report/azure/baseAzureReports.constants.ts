import { PREV_REPORT_TRANSFORMER, TRANSFORM_LEGEND_LABEL } from "dashboard/constants/applications/names";
import { REQUIRED_ONE_FILTER, REQUIRED_ONE_FILTER_KEYS } from "dashboard/constants/filter-key.mapping";
import { FEBasedFilterMap } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { Dict } from "types/dict";
import { BaseReportTypes } from "../baseReport.constant";

export interface BaseAzureReportTypes extends BaseReportTypes {
  across?: string[];
  xaxis?: boolean;
  filterOptionMap?: Dict<string, string>;
  partial_filter_key?: string;
  show_settings_tab?: boolean;
  show_metrics_tab?: boolean;
  HIDE_REPORT?: boolean;
  [PREV_REPORT_TRANSFORMER]?: (data: any) => any;
  fe_based_filters?: FEBasedFilterMap;
  API_BASED_FILTER: string[];
  FIELD_KEY_FOR_FILTERS: { [x: string]: string };
  widget_filter_transform?: (filters: Record<string, any>) => Record<string, any>;
  [REQUIRED_ONE_FILTER]?: (config: any, query: any, report: string) => void;
  [REQUIRED_ONE_FILTER_KEYS]?: Array<string>;
  [TRANSFORM_LEGEND_LABEL]?: (value: string, legendProps: any) => void;
}
