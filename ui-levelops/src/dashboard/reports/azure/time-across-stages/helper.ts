import { AZURE_TIME_FILTERS_KEYS } from "constants/filters";
import { get } from "lodash";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "utils/dateUtils";
import { TIME_ACROSS_STAGES_ID_FILTER } from "./constant";

export const timeAcrossStagesXAxisLabelTransform = (params: any) => {
  const { across, item = {} } = params;
  const { key, additional_key } = item;
  let newLabel = key || additional_key;
  if (TIME_ACROSS_STAGES_ID_FILTER.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  if (["priority"].includes(across)) {
    newLabel = get(staticPriorties, [key], key);
  }
  if (across === "code_area") {
    newLabel = key?.split("\\").pop() || key;
  }

  return newLabel;
};

export const timeAcrossStagesOnChartClickPayload = (params: any) => {
  const { data, across } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (TIME_ACROSS_STAGES_ID_FILTER.includes(across)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  } else if (across && AZURE_TIME_FILTERS_KEYS.includes(across)) {
    const newData = data?.activePayload?.[0]?.payload;
    return convertEpochToDate(newData.key, DEFAULT_DATE_FORMAT, true);
  }
  return keyValue;
};
