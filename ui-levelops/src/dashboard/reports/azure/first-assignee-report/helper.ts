import { idFilters } from "dashboard/reports/jira/commonJiraReports.constants";
import { get } from "lodash";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";

export const xAxisLabeltransform = (params: any) => {
  const { interval, across, item = {} } = params;
  const { key, additional_key } = item;
  let newLabel = key;
  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  if (["priority"].includes(across)) {
    newLabel = get(staticPriorties, [key], key);
  }

  if (across === "code_area") {
    newLabel = key.split("\\").pop() || key;
  }

  return newLabel;
};
