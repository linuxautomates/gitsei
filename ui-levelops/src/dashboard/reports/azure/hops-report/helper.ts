import { get } from "lodash";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { AZURE_ID_FILTERS } from "../constant";

export const xAxisLabelTransform = (params: any) => {
  const { interval, across, item = {} } = params;
  const { key, additional_key } = item;
  let newLabel = key;

  if (AZURE_ID_FILTERS.includes(across)) {
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
