import { ColumnProps } from "antd/lib/table";
import {
  CHART_TOOLTIP_RENDER_TRANSFORM,
  CHART_X_AXIS_TITLE_TRANSFORMER,
  CHART_X_AXIS_TRUNCATE_TITLE
} from "dashboard/constants/applications/names";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { TooltipListItemType } from "shared-resources/charts/components/GenericTooltipRenderer";
import { Dict } from "types/dict";

export type ignoreFilterKeysType = {
  ignoreCommonFilterKeys?: string[];
  ignoreCustomFilterKeys?: string[];
};

export type filterMapping = Dict<string, string>;

export type filterOptionsType = { [x: string]: { key: string }[] };

export type chartPropsType = {
  barGap?: number;
  margin?: {
    top: number;
    bottom: number;
    right: number;
    left: number;
  };
  className?: string;
};

export type WidgetChartDataTransformerType = {
  // * currentValue = current x axis label value
  // * payload = record for that particular x axis
  // * dataKey = x Axis dataKey for the chart
  [CHART_X_AXIS_TITLE_TRANSFORMER]: (currentValue: string, payload: { [x: string]: any }, dataKey: string) => string;

  // * if we want to truncate x axis title
  [CHART_X_AXIS_TRUNCATE_TITLE]: boolean;

  // * if we want to render tooltip differently
  [CHART_TOOLTIP_RENDER_TRANSFORM]: (
    payload: { [x: string]: any },
    currentLabel: string
  ) => { header: React.ReactNode; list?: TooltipListItemType[] };
};

export type optionType = {
  label: string;
  value: string;
};

export type selectRestApiOptionType = Omit<optionType, "value"> & {
  key: string;
};

export type FiltersType = { product_id?: string; integration_ids?: string[] };

export type basicActionType<T> = {
  type: string;
  id: string;
  uri: string;
  data?: T;
  method: string;
  queryparams?: basicMappingType<T>;
  extra?: basicMappingType<any>;
};

export type basicMappingType<T> = {
  [x in string]: T;
};

export type basicRangeType = {
  $lt: string | number;
  $gt: string | number;
};

export type CustomPaginationConfig = {
  page: number;
  pageSize: number;
};

export type SortOptionType = Array<{ id: string; desc: boolean }>;

/** drilldown columns transform as per reports type */
export interface ExtraColumnProps {
  filterLabel: string;
  filterType: string;
  filterField: string;
  titleForCSV?: string;
  options?: Array<any>;
}
export interface ReportDrilldownColTransFuncParams {
  columns: Array<ExtraColumnProps & ColumnProps<any>>;
  tableRecords?: Array<basicMappingType<any>>; // records of drilldown columns
  categoryColorMapping?: basicMappingType<string>; // mapping of Effort Investment Profile Category with it's corresponding color
  filters?: any;
  changeUriToggle?: boolean;
  doraProfileDeploymentRoute?: any;
  doraProfileEvent?: string;
}
export interface ReportDrilldownFilterTransFuncParams {
  filters?: any;
}

export type ReportDrilldownColTransFuncType = (utilities: ReportDrilldownColTransFuncParams) => Array<ColumnProps<any>>;
export type ReportDrilldownFilterTransFuncType = (
  utilities: ReportDrilldownFilterTransFuncParams
) => supportedFilterType;
