import { basicMappingType, SortOptionType } from "dashboard/dashboard-types/common-types";
import { BaseReportTypes } from "../baseReport.constant";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY,
  REQUIRED_ONE_FILTER,
  REQUIRED_ONE_FILTER_KEYS
} from "../../../dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "../../../dashboard/constants/filter-name.mapping";
import {
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  TRANSFORM_LEGEND_LABEL
} from "../../../dashboard/constants/applications/names";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "dashboard/reports/jira/constant";

/** use this for adding keys which are common among jira application. */
export interface BaseJiraReportTypes extends BaseReportTypes {
  defaultSort?: SortOptionType;
  compareField?: string;
  supported_widget_types?: Array<string>;
  chart_click_enable?: boolean;
  show_max?: boolean;
  jira_or_filter_key?: string;
  xAxisLabelTransform?: (params: any) => void;
  onChartClickPayload?: (params: { data: basicMappingType<any>; across?: string }) => string | object;
  [API_BASED_FILTER]?: string[];
  [FIELD_KEY_FOR_FILTERS]?: { [x: string]: string };
  [FILTER_NAME_MAPPING]?: { [x: string]: string };
  [PREV_REPORT_TRANSFORMER]?: (data: any) => any;
  [MULTI_SERIES_REPORT_FILTERS_CONFIG]?: LevelOpsFilter[];
  [PARTIAL_FILTER_MAPPING_KEY]?: Record<string, string>;
  [PARTIAL_FILTER_KEY]?: string;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]?: Record<string, any>;
  [REQUIRED_ONE_FILTER]?: (config: any, query: any, report: string) => void;
  [REQUIRED_ONE_FILTER_KEYS]?: Array<string>;
  [TRANSFORM_LEGEND_LABEL]?: (value: string, legendProps: any) => void;
}
