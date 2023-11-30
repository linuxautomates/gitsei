import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { BaseDoraReportTypes } from "../baseDORAreport.constants";
import { Dict } from "types/dict";
import { FILTER_WITH_INFO_MAPPING, filterWithInfoType } from "dashboard/constants/filterWithInfo.mapping";
import { PREVIEW_DISABLED } from "dashboard/constants/applications/names";
import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { FC } from "react";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";

export interface DoraMeanTimeToRestoreReportType extends BaseDoraReportTypes {
  getDoraProfileIntegrationType: (param: any) => string;
  getDrilldownTitle: (param: any) => string;
  dataKey:string;
  [FILTER_NAME_MAPPING]:  Dict<string, string>;
  [FILTER_WITH_INFO_MAPPING]: any;
  [PREVIEW_DISABLED]: boolean;
  valuesToFilters: Dict<string, string>;
  [PARTIAL_FILTER_KEY]: string;
  includeContextFilter:boolean;
  drilldownFooter: () => FC<any>;
  drilldownCheckbox: () => FC<any>;
  drilldownTotalColCaseChange: boolean;
  getDoraProfileEvent?:(params:any) => string;
  getChartProps?:(params:any) => any;
  [STORE_ACTION]: Function;
  onChartClickPayload: Function;
  keysToNeglect:string[];
  getDoraProfileIntegrationApplication: Function;
  getDoraLeadTimeMeanTimeData: Function;
}