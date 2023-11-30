import { get } from "lodash";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";

export const xAxisLableTransform = (params: any) => {
  const { across, item = {} } = params;
  const { key } = item;
  let newLabel = key;
  if (["priority"].includes(across)) {
    newLabel = get(staticPriorties, [key], key);
  }

  if (across === "code_area") {
    newLabel = key.split("\\").pop() || key;
  }

  return newLabel;
};
