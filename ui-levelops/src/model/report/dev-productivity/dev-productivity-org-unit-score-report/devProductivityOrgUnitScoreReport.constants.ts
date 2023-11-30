import { BaseDevProductivityReportTypes } from "../baseDevProductivityReports.constants";
import { CSV_DRILLDOWN_TRANSFORMER, IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { GET_GRAPH_FILTERS, GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import { GET_WIDGET_CHART_PROPS } from "../../../../dashboard/constants/filter-name.mapping";

export interface DevProductivityOrgUnitScoreReportType extends BaseDevProductivityReportTypes {
  xaxis: boolean;
  uri: string;
  getDynamicColumns: (data: { [key: string]: any }[]) => { [key: string]: any }[];
  [CSV_DRILLDOWN_TRANSFORMER]: (data: any) => void;
  [GET_GRAPH_FILTERS]: (data: any) => any;
  [GET_WIDGET_CHART_PROPS]: (data: any) => any;
  [IS_FRONTEND_REPORT]: boolean;
  [GET_WIDGET_TITLE_INTERVAL]?: boolean;
}
