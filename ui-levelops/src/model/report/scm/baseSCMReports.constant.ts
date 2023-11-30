import { basicMappingType, SortOptionType } from "dashboard/dashboard-types/common-types";
import { BaseReportTypes } from "../baseReport.constant";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../dashboard/constants/filter-key.mapping";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING, VALUE_SORT_KEY } from "../../../dashboard/constants/filter-name.mapping";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

/** use this for adding keys which are common among SCM application. */
export interface BaseSCMReportTypes extends BaseReportTypes {
  xaxis?: boolean;
  defaultSort?: SortOptionType;
  supported_widget_types?: Array<string>;
  chart_click_enable?: boolean;
  show_max?: boolean;
  hide_custom_fields?: boolean;
  disable_partial_filter_mapping_key?: Array<String>;
  xAxisLabelTransform?: (params: any) => void;
  onChartClickPayload?: (params: { data: basicMappingType<any>; across?: string }) => string | object;
  [API_BASED_FILTER]?: string[];
  [FIELD_KEY_FOR_FILTERS]?: { [x: string]: string };
  [FILTER_NAME_MAPPING]?: { [x: string]: string };
  [PARTIAL_FILTER_MAPPING_KEY]?: Record<string, string>;
  [PARTIAL_FILTER_KEY]?: string;
  valuesToFilters?: { [x: string]: string };
  [PREV_REPORT_TRANSFORMER]?: (data: any) => any;
  [ALLOWED_WIDGET_DATA_SORTING]?: boolean;
  [VALUE_SORT_KEY]?: string;
}
export const SHOW_SINGLE_STAT_EXTRA_INFO = "SHOW_SINGLE_STAT_EXTRA_INFO";

enum TagType {
  "ELITE",
  "HIGH",
  "LOW"
}
export interface SCMSingleStatExtraInfo {
  count: number;
  discription: string;
  type: {
    previousType: TagType;
    currentType: TagType;
  };
}
