import {
  BA_TIME_RANGE_FILTER_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { basicMappingType, basicRangeType } from "dashboard/dashboard-types/common-types";

export interface EISingleStatFiltersType {
  initialFilters: basicMappingType<any>;
  issueResolvedAtTimeRange?: basicRangeType;
  statusCategoriesKey?: string;
  statusCategoriesValue?: Array<string>;
  issueResolvedAtFilterKey: string;
  acrossValue: string;
  eiProfileKey: string;
  keysToUnset: Array<string>;
}

export const eiSingleStateFiltersDefaultMapping: EISingleStatFiltersType = {
  initialFilters: {},
  issueResolvedAtFilterKey: "issue_resolved_at",
  acrossValue: "ticket_category",
  eiProfileKey: TICKET_CATEGORIZATION_SCHEMES_KEY,
  keysToUnset: [BA_TIME_RANGE_FILTER_KEY, TICKET_CATEGORIZATION_UNIT_FILTER_KEY]
};
