import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { LevelOpsFilter } from "../../filters/levelopsFilters";
import { ReportTransformFuncParamsTypes } from "../baseReport.constant";
import { DEPRECATED_REPORT } from "dashboard/constants/applications/names";

export interface BasePagerdutyReportTypes {
  name: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  method: string;
  uri?: string;
  xaxis: boolean;
  chart_props?: basicMappingType<any>;
  filters?: basicMappingType<any>;
  defaultAcross?: string;
  default_query?: basicMappingType<any>;
  composite?: boolean;
  composite_transform?: basicMappingType<string>;
  defaultFilterKey?: string;
  convertTo?: "days" | "mins" | "seconds" | "hours";

  /** @todo we'll make it a mandatory field. It is optional temparory. */
  report_filters_config?: LevelOpsFilter[];
  transformFunction?: (data: ReportTransformFuncParamsTypes) => any;
  [DEPRECATED_REPORT]?: boolean;
}
