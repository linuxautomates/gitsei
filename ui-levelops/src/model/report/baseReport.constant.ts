import { CustomDrillDownType } from "dashboard/constants/drilldown.constants";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { ChartContainerType } from "dashboard/helpers/helper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

export interface ReportTransformFuncParamsTypes {
  reportType: string;
  apiData: any;
  metadata: basicMappingType<any>;
  records: number;
  filters: basicMappingType<any>;
  widgetFilters: basicMappingType<any>;
  statUri: string;
  uri: string;
  sortBy: string;
  isMultiTimeSeriesReport: boolean;
  dashMeta: basicMappingType<any>;
  supportedCustomFields: Array<basicMappingType<any>>;
}
export type FilterConfigBasedPreviewFilterConfigType = { filter_key: string; valueKey: string; labelKey: string };

/** use this as a base typing for reports. */
export interface BaseReportTypes {
  name: string;
  description?: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  method: string;
  supported_filters?: supportedFilterType;
  drilldown: CustomDrillDownType | {};
  uri?: string;
  chart_props?: basicMappingType<any>;
  filters?: basicMappingType<any>;
  defaultAcross?: string;
  default_query?: basicMappingType<any>;
  composite?: boolean;
  composite_transform?: basicMappingType<string>;
  defaultFilterKey?: string;
  convertTo?: "days" | "mins" | "seconds" | "hours";
  getChartUnits?: (params: any) => string[];
  filter_config_based_preview_filters?: FilterConfigBasedPreviewFilterConfigType[];

  /** @todo we'll make it a mandatory field. It is optional temparory. */
  report_filters_config?: Function | LevelOpsFilter[];
  transformFunction?: (data: ReportTransformFuncParamsTypes) => any;
}

/** this type is used for STORE_ACTION field in report constant */
export type StoreActionType = (
  uri: string,
  method: string,
  filters: any,
  complete?: any,
  id?: string,
  extra?: any
) => {
  type: string;
  data?: any;
  id?: string;
  uri: string;
  method: string;
  extra?: any;
};
