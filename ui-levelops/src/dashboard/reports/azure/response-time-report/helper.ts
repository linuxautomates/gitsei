import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { ATTRIBUTE_FILTERS, AZURE_ID_FILTERS } from "../constant";

export const onChartClick = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (across && (AZURE_ID_FILTERS.includes(across) || ATTRIBUTE_FILTERS.includes(across))) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};

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
