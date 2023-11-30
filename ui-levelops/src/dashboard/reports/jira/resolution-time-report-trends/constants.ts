import { Dict } from "../../../../types/dict";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";

export const JIRA_TIME_ACROSS_FILTER_MAPPING: Dict<string, string> = {
  ...JIRA_COMMON_FILTER_OPTION_MAPPING,
  status: "Current Status"
};
