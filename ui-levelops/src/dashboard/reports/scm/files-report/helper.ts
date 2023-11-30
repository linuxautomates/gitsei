import { get } from "lodash";

export const scmFilesmodulePathFilterVisibilityHelper = (args: any) => {
  const { metadata } = args;
  return !get(metadata, ["groupByRootFolder_scm_files_report"], false);
};

export const scmFilesmodulePathFilterSelectedHelper = (args: any) => {
  const { metadata } = args;
  return get(metadata, ["groupByRootFolder_scm_files_report"], false);
};
