import { basicMappingType, ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import {
  BAR_CHART_REF_LINE_STROKE,
  DEFAULT_METADATA,
  IGNORE_FILTER_KEYS_CONFIG
} from "dashboard/constants/filter-key.mapping";
import { Dict } from "types/dict";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../../../../dashboard/reports/jira/constant";

export interface SprintMetricTrendReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [IGNORE_FILTER_KEYS_CONFIG]: ignoreFilterKeysType;
  [DEFAULT_METADATA]: Dict<string, any>;
  columnWithInformation: boolean;
  columnsWithInfo: basicMappingType<string>;
  [BAR_CHART_REF_LINE_STROKE]: string;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: any;
}
