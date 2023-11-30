import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { Dict } from "../../types/dict";

export const chartProps: basicMappingType<any> = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const includeSolveTimeImplicitFilter = {
  include_solve_time: true
};

const scmPRbrnachFilterMapping = {
  source_branch: "Source Branch",
  target_branch: "Destination Branch"
};
export const JIRA_COMMON_FILTER_OPTION_MAPPING: Dict<string, string> = {
  version: "Affects Version",
  ...scmPRbrnachFilterMapping
};
