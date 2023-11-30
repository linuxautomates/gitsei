import { BaseDevProductivityReportTypes } from "../baseDevProductivityReports.constants";
import { GET_CUSTOMIZE_TITLE, IS_FRONTEND_REPORT } from "dashboard/constants/filter-key.mapping";
import { GET_GRAPH_FILTERS } from "dashboard/constants/applications/names";
import { ReactNode } from "react";

export interface DevProductivityPRActivityReportType extends BaseDevProductivityReportTypes {
  [GET_CUSTOMIZE_TITLE]: (title: string, dashboardTimeRange: any) => ReactNode | string;
  getDynamicColumns: (data: { [key: string]: any }[], contextFilter: any) => { [key: string]: any }[];
  [GET_GRAPH_FILTERS]: (data: any) => any;
  [IS_FRONTEND_REPORT]: boolean;
  height: string;
}
