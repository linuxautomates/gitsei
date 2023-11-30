import DrilldownViewMissingCheckbox from "dashboard/reports/jira/lead-time-by-stage-report/DrilldownViewMissingCheckbox";
import { isArray } from "lodash";
export const getDrilldownCheckBox = () => {
  return DrilldownViewMissingCheckbox;
};

export const overrideFilterWithStackFilter = (filters: any, drillDownProps: any) => {
  const { stackBy } = drillDownProps.widgetMetaData;

  if (isArray(stackBy) && stackBy[0]) {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        [stackBy[0]]: drillDownProps.stackFilters
      }
    };
  }

  return filters;
};
