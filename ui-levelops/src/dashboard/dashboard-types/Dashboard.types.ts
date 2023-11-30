import { ColumnProps } from "antd/lib/table";
import { basicMappingType, optionType } from "./common-types";
import { EXCLUDE_SUB_COLUMNS_FOR, SUB_COLUMNS_TITLE } from "../constants/bussiness-alignment-applications/constants";

export type DashboardHeaderConfigType = {
  dashCount: number;
  dashboardTitle: string;
  style?: { [x: string]: string };
};

export type CopyDestinationDashboardConfig = {
  selectedDashboard: string;
  selectedRowIndex: number;
  selectedPage: number;
};

export type dropdownWithTagSelectConfig = {
  uri: string;
  placeholder?: string;
  searchField?: string;
  mode?: string;
  className?: string;
  allowClear?: boolean;
  uuid?: string;
  showSearch?: boolean;
  dropdownClassName?: string;
  showSpinnerWhenLoading?: boolean;
  useOnSelect?: boolean;
  specialKey?: string;
  filterOptionMethod?: (data: any) => boolean;
  transformOptions?: (data: any[]) => optionType[];
};

/**
 * Types for widget CSV Export
 */

export type dynamicURIFuncType = (
  filters: basicMappingType<any>,
  metadata: basicMappingType<any>,
  reportType: string
) => string;

export type filtersTransformationUtilityType = {
  widgetMetadata: basicMappingType<any>;
  dashboardMetadata: basicMappingType<any>;
  uri: string;
  report: string;
  application: string;
  supportedCustomFields: Array<{ name: string; field_key: string }>;
  queryParam?: any;
  widgetId?: any;
};

export type filtersTransformerType = (
  filters: basicMappingType<any>,
  utilityConfig: filtersTransformationUtilityType
) => basicMappingType<any>;

export type ReportCSVDownloadConfig = {
  widgetFiltersTransformer?: filtersTransformerType;
  widgetDynamicURIGetFunc?: dynamicURIFuncType;
  widgetCSVColumnsGetFunc?: (data: Array<basicMappingType<any>> | basicMappingType<any>) => ColumnProps<any>[];
  widgetCSVDataTransform?: (
    data: Array<basicMappingType<any>> | basicMappingType<any>,
    columns: ColumnProps<any>[]
  ) => any[];
  [SUB_COLUMNS_TITLE]?: string[];
  [EXCLUDE_SUB_COLUMNS_FOR]?: string[];
};

/** types for dev productivity */
export type scoreCardDashboardConfigType = {
  dashboardId: string;
  type: string;
  name: string;
  showActions: boolean;
  metadata: {
    dashboard_time_range: boolean;
    dashboard_time_range_filter: string;
  };
};

export type Metadata = {
  dashboard_time_range_filter: string;
  dashboard_time_range?: string;
};
