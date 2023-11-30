import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { Dict } from "types/dict";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";

export interface StageBounceSingleStatType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FE_BASED_FILTERS]: Dict<string, any>;
  drilldown: any;
  valuesToFilters: Dict<string, string>;
  requiredFilters: string[];
  [WIDGET_VALIDATION_FUNCTION]: (params: any) => boolean;
  supportExcludeFilters: boolean;
  supportPartialStringFilters: boolean;
}
