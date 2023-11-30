import {
  CATEGORY,
  FIELD_KEY_FOR_FILTERS,
  GET_CUSTOMIZE_TITLE,
  IS_FRONTEND_REPORT
} from "dashboard/constants/filter-key.mapping";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { REPORT_CSV_DOWNLOAD_CONFIG, WIDGET_MIN_HEIGHT } from "dashboard/constants/bussiness-alignment-applications/constants";
import { CUSTOM_FIELD_KEY } from "dashboard/reports/jira/lead-time-by-time-spent-in-stages/constant";

export interface jiraReleaseTableReportTypes extends BaseJiraReportTypes {
  [IS_FRONTEND_REPORT]: boolean;
  [CATEGORY]: string;
  [FIELD_KEY_FOR_FILTERS]: Record<string, string>;
  mapFiltersBeforeCall: (args: any, value: any) => any;
  mapFiltersForWidgetApi: (args: any, value: any) => any;
  widgetTableColumn: any;
  [REPORT_CSV_DOWNLOAD_CONFIG]: any;
  [GET_CUSTOMIZE_TITLE]?: (args: any, value: any) => any;
  hasPaginationTableOnWidget: boolean;
  getDrilldownTitle: (args: any, value: any) => any;
  getDrillDownType: (args: any, value: any) => any;
  [CUSTOM_FIELD_KEY]: string;
  [WIDGET_MIN_HEIGHT]: string;
}
