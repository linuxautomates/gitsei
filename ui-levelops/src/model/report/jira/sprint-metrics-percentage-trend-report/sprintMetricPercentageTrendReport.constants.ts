import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { DEFAULT_METADATA, IGNORE_FILTER_KEYS_CONFIG } from "dashboard/constants/filter-key.mapping";
import { Dict } from "types/dict";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../../../../dashboard/reports/jira/constant";

export interface SprintMetricPercentageTrendReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [IGNORE_FILTER_KEYS_CONFIG]: Dict<string, any>;
  [DEFAULT_METADATA]: Dict<string, any>;
  columnWithInformation: boolean;
  columnsWithInfo: basicMappingType<string>;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: any;
}
