import { BaseDevProductivityReportTypes } from "../baseDevProductivityReports.constants";
import { CSV_DRILLDOWN_TRANSFORMER, GET_CUSTOMIZE_TITLE } from "../../../../dashboard/constants/filter-key.mapping";
import { GET_GRAPH_FILTERS, GET_WIDGET_TITLE_INTERVAL } from "../../../../dashboard/constants/applications/names";
import {
  GET_WIDGET_CHART_PROPS,
  WIDGET_FILTER_PREVIEW_COUNT
} from "../../../../dashboard/constants/filter-name.mapping";
import { IS_FRONTEND_REPORT } from "../../../../dashboard/constants/filter-key.mapping";

export interface DevProductivityScoreReportType extends BaseDevProductivityReportTypes {
  xaxis: boolean;
  uri: string;
  [CSV_DRILLDOWN_TRANSFORMER]: (data: any) => void;
  getDynamicColumns: (data: { [key: string]: any }[]) => { [key: string]: any }[];
  [GET_GRAPH_FILTERS]: (data: any) => any;
  [GET_WIDGET_CHART_PROPS]: (data: any) => any;
  [WIDGET_FILTER_PREVIEW_COUNT]: (filters: any) => number;
  [IS_FRONTEND_REPORT]: boolean;
  [GET_WIDGET_TITLE_INTERVAL]: any;
  [GET_CUSTOMIZE_TITLE]: any;
}
