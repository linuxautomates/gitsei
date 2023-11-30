import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { NO_LONGER_SUPPORTED_FILTER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  API_BASED_FILTER,
  AVAILABLE_COLUMNS,
  DEFAULT_COLUMNS,
  FIELD_KEY_FOR_FILTERS,
  IS_FRONTEND_REPORT,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { REPORT_HEADER_INFO } from "dashboard/reports/azure/program-progress-report/constant";
import React from "react";
import {
  DefaultKeyTypes,
  MAX_RECORDS_LABEL,
  MAX_RECORDS_OPTIONS_KEY,
  STORE_ACTION,
  SUPPORT_CATEGORY_EPIC_ACROSS_FILTER,
  URI_MAPPING,
  WIDGET_MIN_HEIGHT
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { ColumnProps } from "antd/lib/table";

export type SetSortConfigFuncType = (key: string, order: string, noSorting?: boolean) => void;
export interface AzureProgramProgressReportType extends BaseAzureReportTypes {
  [API_BASED_FILTER]: string[];
  [FIELD_KEY_FOR_FILTERS]: { [x: string]: string };
  [REPORT_FILTERS_CONFIG]: any;
  [REPORT_HEADER_INFO]: (params: any) => React.ReactElement | null;
  [IS_FRONTEND_REPORT]: boolean;
  displayColumnSelection: boolean;
  [AVAILABLE_COLUMNS]: any;
  [DEFAULT_COLUMNS]: any;
  available_columns_func: (setSortConfig: SetSortConfigFuncType) => Array<ColumnProps<any>>;
  supportExcludeFilters: boolean;
  supportPartialStringFilters: boolean;
  [PARTIAL_FILTER_MAPPING_KEY]: { [x: string]: string };
  show_max: boolean;
  [MAX_RECORDS_OPTIONS_KEY]: { [x: string]: string | number }[];
  [MAX_RECORDS_LABEL]: string;
  [WIDGET_MIN_HEIGHT]: string;
  [STORE_ACTION]: any;
  [SUPPORT_CATEGORY_EPIC_ACROSS_FILTER]: boolean;
  [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: string;
  [URI_MAPPING]: { [x: string]: string | number };
  onChartClickPayload: (params: { [key: string]: any }) => any;
  valuesToFilters: { [x: string]: string };
  removeHiddenFiltersFromPreview: (params: any) => any;
  [NO_LONGER_SUPPORTED_FILTER]: (params: any) => any;
}
