import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";

export interface JiraIssuesReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  storyPointUri: string;
  allow_key_for_stacks: boolean;
  weekStartsOnMonday: boolean;
  infoMessages: basicMappingType<string>;
  getStacksStatus: (filters: basicMappingType<any>) => boolean;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  getTotalLabel?: (data: any) => string;
  stack_filters?: Array<string>;
  ADD_EXTRA_FILTER?: any;
  mapFiltersBeforeCall?: (filter: any) => any;
  maxStackEntries: number;
  generateBarColors: (dataKey: string) => string;
  transformFinalFilters: (finalFilters: any, transformFinalFilters: any) => any;
}
