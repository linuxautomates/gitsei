import { ReportTransformFuncParamsTypes } from "../baseReport.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../../dashboard/helpers/helper";
import { CustomDrillDownType } from "../../../dashboard/constants/drilldown.constants";
import { basicMappingType } from "../../../dashboard/dashboard-types/common-types";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { SHOW_AGGREGATIONS_TAB, SHOW_METRICS_TAB } from "dashboard/constants/filter-key.mapping";
import { ReactNode } from "react";

export type EmptyWidgetPreviewArgsType = { errorCode: number; chartType?: ChartType };
export interface BaseDevProductivityReportTypes {
  name: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  method: string;
  drilldown?: CustomDrillDownType | {};
  uri: string;
  chart_props: basicMappingType<any>;
  filters: basicMappingType<any>;
  default_query?: basicMappingType<any>;
  show_notification_on_error?: boolean;
  transformFunction: (data: ReportTransformFuncParamsTypes) => any;
  render_empty_widget_preview_func?: (args: EmptyWidgetPreviewArgsType) => ReactNode;
  [WIDGET_MIN_HEIGHT]?: string;
  defaultSort?: any;
  [SHOW_METRICS_TAB]?: boolean;
  [SHOW_AGGREGATIONS_TAB]?: boolean;
}
