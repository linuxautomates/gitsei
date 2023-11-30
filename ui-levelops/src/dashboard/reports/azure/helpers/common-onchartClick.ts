import { AZURE_TIME_FILTERS_KEYS } from "constants/filters";
import { get } from "lodash";
import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "utils/dateUtils";
import { ATTRIBUTE_FILTERS, AZURE_ID_FILTERS } from "../constant";

export const onChartClickPayloadForId = (params: any) => {
  const { data, across } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (across && (AZURE_ID_FILTERS.includes(across) || ATTRIBUTE_FILTERS.includes(across))) {
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
