import { get } from "lodash";

export const modulePathFilterVisibilityHelper = (args: any) => {
  const { metadata } = args;
  return !get(metadata, ["groupByRootFolder_scm_jira_files_report"], false);
};

export const modulePathFilterSelectedHelper = (args: any) => {
  const { metadata } = args;
  return get(metadata, ["groupByRootFolder_scm_jira_files_report"], false);
};
