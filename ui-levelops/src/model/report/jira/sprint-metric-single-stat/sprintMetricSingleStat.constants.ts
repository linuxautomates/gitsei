import { basicMappingType, ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  IGNORE_FILTER_KEYS_CONFIG,
  DEFAULT_METADATA
} from "dashboard/constants/filter-key.mapping";
import { Dict } from "types/dict";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../../../../dashboard/reports/jira/constant";

export interface SprintMetricSingleStatType extends BaseJiraReportTypes {
  xaxis: boolean;
  columnWithInformation: boolean;
  columnsWithInfo: basicMappingType<string>;
  [CSV_DRILLDOWN_TRANSFORMER]: (data: basicMappingType<any>) => string;
  [IGNORE_FILTER_KEYS_CONFIG]: ignoreFilterKeysType;
  [DEFAULT_METADATA]: Dict<string, any>;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: any;
}
