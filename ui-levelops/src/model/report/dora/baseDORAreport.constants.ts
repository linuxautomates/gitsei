import { ReactNode } from "react";
import { BaseReportTypes } from "../baseReport.constant";
import {
  IS_FRONTEND_REPORT,
  SHOW_SETTINGS_TAB,
  CATEGORY,
  SHOW_FILTERS_TAB,
  SHOW_METRICS_TAB,
  SHOW_WEIGHTS_TAB,
  SHOW_AGGREGATIONS_TAB,
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  GET_CUSTOMIZE_TITLE,
  CSV_DRILLDOWN_TRANSFORMER,
  DEFAULT_METADATA
} from "dashboard/constants/filter-key.mapping";
import { GET_GRAPH_FILTERS, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { Dict } from "types/dict";

export interface BaseDoraReportTypes extends BaseReportTypes {
  [SHOW_SETTINGS_TAB]?: boolean;
  [SHOW_FILTERS_TAB]?: boolean;
  [SHOW_METRICS_TAB]?: boolean;
  [SHOW_WEIGHTS_TAB]?: boolean;
  [SHOW_AGGREGATIONS_TAB]?: boolean;
  [IS_FRONTEND_REPORT]?: boolean;
  [CATEGORY]: string;
  [API_BASED_FILTER]?: string[];
  [FIELD_KEY_FOR_FILTERS]?: { [x: string]: string };
  [GET_CUSTOMIZE_TITLE]?: (title: string, dashboardTimeRange: any) => ReactNode | string;
  conditionalUriMethod?: (data: any) => string;
  filterWarningLabel?: string;
  [GET_GRAPH_FILTERS]?: (params: any) => any;
  drilldownMissingAndOtherRatings?: boolean;
  getShowTitle?: (params: any) => boolean;
  hideFilterButton?: () => boolean;
  [CSV_DRILLDOWN_TRANSFORMER]?: Function;
  getFilterKeysToHide?: (params: any) => string[];
  drilldownToggleConfig?:  {
    getToggleComponent:() => React.FC<any>,
    title: string,
    initialValue: boolean,
    onChangeHandler: Function
  };
  [DEFAULT_METADATA]?: any;
  [PREV_REPORT_TRANSFORMER]?: Function,
  getDefaultMetadata?:Function
  getDoraProfileDeploymentRoute?:Function,
  filterOptionMap?: Dict<string, string>;
}
