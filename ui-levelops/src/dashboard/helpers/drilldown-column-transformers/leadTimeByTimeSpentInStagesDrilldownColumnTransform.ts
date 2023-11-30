import { ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";

/**
 * This TypeScript function filters columns based on whether they are included in a list of hidden
 * stages.
 * @param  - - `leadTimeByTimeSpentInStagesDynamicColTransform`: This is a function that takes in an
 * object with `columns` and `filters` properties and returns an array of columns.
 * @returns The function `leadTimeByTimeSpentInStagesDynamicColTransform` returns an array of columns
 * that are not included in the `hide_stages` array obtained from the `metadata` object in the
 * `filters` parameter. The columns are filtered based on whether their `titleForCSV` property is
 * included in the `hideStages` array.
 */
export const leadTimeByTimeSpentInStagesDynamicColTransform: ReportDrilldownColTransFuncType = ({
  columns,
  filters
}) => {
  const { metadata } = filters;
  const hideStages: string[] = get(metadata, ["hide_stages"], []);
  return columns.filter(col => !hideStages.includes(col.titleForCSV ?? ""));
};
