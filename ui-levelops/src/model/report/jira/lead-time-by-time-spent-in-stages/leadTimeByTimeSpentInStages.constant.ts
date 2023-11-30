import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  FIELD_KEY_FOR_FILTERS,
  IS_FRONTEND_REPORT
} from "dashboard/constants/filter-key.mapping";
import { METADATA_FILTERS_PREVIEW } from "dashboard/constants/filter-name.mapping";
import { CUSTOM_FIELD_KEY } from "dashboard/reports/jira/lead-time-by-time-spent-in-stages/constant";
import { StoreActionType } from "model/report/baseReport.constant";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface LeadTimeByTimeSpentInStagesReportTypes extends BaseJiraReportTypes {
  [IS_FRONTEND_REPORT]: boolean;
  [FIELD_KEY_FOR_FILTERS]: Record<string, string>;
  [METADATA_FILTERS_PREVIEW]: (args: any) => { label: string; value: string }[];
  [CSV_DRILLDOWN_TRANSFORMER]: (args: any) => any;
  [STORE_ACTION]: StoreActionType;
  [CUSTOM_FIELD_KEY]: string;
  mapFiltersForWidgetApi: (args: any, value: any) => any;
  mapFiltersBeforeCall: (args: any, value: any) => any;
  getDrilldownTitle: any;
  getDrillDownType?: () => string;
}
