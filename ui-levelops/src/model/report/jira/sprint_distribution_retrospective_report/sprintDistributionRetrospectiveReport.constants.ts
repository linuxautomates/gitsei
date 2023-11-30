import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import {
  ALLOW_KEY_FOR_STACKS,
  BAR_CHART_REF_LINE_STROKE,
  DEFAULT_METADATA,
  IGNORE_FILTER_KEYS_CONFIG,
  SHOW_AGGREGATIONS_TAB,
  SHOW_FILTERS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB,
  SHOW_WEIGHTS_TAB
} from "dashboard/constants/filter-key.mapping";
import { ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "../../../../dashboard/reports/jira/constant";

export interface SprintDistributionRetrospectiveReportTypes extends BaseJiraReportTypes {
  xaxis?: boolean;
  show_max: boolean;
  [IGNORE_FILTER_KEYS_CONFIG]: ignoreFilterKeysType;
  columnWithInformation: boolean;
  columnsWithInfo: any;
  doneStatusFilter: { valueKey: string };
  [ALLOW_KEY_FOR_STACKS]: boolean;
  [SHOW_SETTINGS_TAB]: boolean;
  [SHOW_METRICS_TAB]: boolean;
  [SHOW_FILTERS_TAB]: boolean;
  [SHOW_WEIGHTS_TAB]: boolean;
  [SHOW_AGGREGATIONS_TAB]: boolean;
  [BAR_CHART_REF_LINE_STROKE]: string;
  [DEFAULT_METADATA]: any;
  [FE_BASED_FILTERS]: any;
  supportExcludeFilters: boolean;
  supportPartialStringFilters: boolean;
  [WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY]: any;
}
