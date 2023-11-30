import { GET_FILTERS, GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import { CustomDrillDownType } from "dashboard/constants/drilldown.constants";
import {
  SHOW_METRICS_TAB,
  SHOW_AGGREGATIONS_TAB,
  IS_FRONTEND_REPORT,
  DEFAULT_COLUMNS,
  AVAILABLE_COLUMNS,
  GET_CUSTOMIZE_TITLE
} from "dashboard/constants/filter-key.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ColumnPropsWithType } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { ReactNode } from "react";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { EmptyWidgetPreviewArgsType } from "../baseDevProductivityReports.constants";

export interface RawStatsReportType {
  name: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  drilldown?: CustomDrillDownType | {};
  chart_props?: basicMappingType<any>;
  getChartProps?: (widgetDetails: any) => basicMappingType<any>;
  filters: basicMappingType<any>;
  [WIDGET_MIN_HEIGHT]?: string;
  defaultSort?: any;
  [SHOW_METRICS_TAB]?: boolean;
  [SHOW_AGGREGATIONS_TAB]?: boolean;
  [GET_FILTERS]?: (data: any) => any;
  [IS_FRONTEND_REPORT]?: true;
  default_query?: any;
  render_empty_widget_preview_func?: (args: EmptyWidgetPreviewArgsType) => ReactNode;
  [AVAILABLE_COLUMNS]: Array<ColumnPropsWithType<any>>;
  [DEFAULT_COLUMNS]: Array<ColumnPropsWithType<any>>;
  displayColumnSelection: boolean;
  [GET_WIDGET_TITLE_INTERVAL]: boolean;
  [GET_CUSTOMIZE_TITLE]?: (data: any) => any;
  widgetEntitlements?: Function;
}
