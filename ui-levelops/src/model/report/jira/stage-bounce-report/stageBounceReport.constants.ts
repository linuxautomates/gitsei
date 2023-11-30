import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import {
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  STACKS_SHOW_TAB
} from "../../../../dashboard/constants/filter-key.mapping";
import { WIDGET_CONFIGURATION_KEYS } from "../../../../constants/widgets";
import {
  FE_BASED_FILTERS,
  INFO_MESSAGES,
  STACKS_FILTER_STATUS
} from "../../../../dashboard/constants/applications/names";

export interface StageBounceReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  stack_filters: string[];
  valuesToFilters: { [x: string]: string };
  tooltipMapping: { [x: string]: string };
  requiredFilters: string[];
  [SHOW_AGGREGATIONS_TAB]: boolean;
  [SHOW_METRICS_TAB]: boolean;
  [STACKS_SHOW_TAB]: WIDGET_CONFIGURATION_KEYS;
  [FILTER_NAME_MAPPING]: { [x: string]: string };
  [WIDGET_VALIDATION_FUNCTION]: (params: any) => boolean;
  getTotalKey: (params: any) => string;
  [STACKS_FILTER_STATUS]: (params: any) => boolean;
  [INFO_MESSAGES]: { [x: string]: string };
  [FE_BASED_FILTERS]: any;
  shouldJsonParseXAxis: () => void;
  supportExcludeFilters: boolean;
  supportPartialStringFilters: boolean;
}
