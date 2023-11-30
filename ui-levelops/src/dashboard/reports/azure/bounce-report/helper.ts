import { AZURE_TIME_FILTERS_KEYS } from "constants/filters";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { idFilters } from "dashboard/reports/jira/commonJiraReports.constants";
import { get } from "lodash";
import moment from "moment";
import { convertEpochToDate, DEFAULT_DATE_FORMAT, isvalidTimeStamp } from "utils/dateUtils";

export const xAxisLabelTransform = (params: any) => {
  const { item = {}, CustomFieldType, xAxisLabelKey, across } = params;
  const { key, additional_key } = item;
  let newLabel = key;

  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }

  const isValidDate = isvalidTimeStamp(key);
  if (CustomFieldType && ((CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) || isValidDate)) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

export const onChartClickHandler = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  let activeLabel = data.key;
  if (across && across.includes("customfield_")) {
    return data.name;
  } else if (across && idFilters.includes(across)) {
    return {
      id: get(data, ["key"], "_UNASSIGNED_"),
      name: get(data, ["name"], "_UNASSIGNED_")
    };
  } else if (across && AZURE_TIME_FILTERS_KEYS.includes(across)) {
    const newData = data?.activePayload?.[0]?.payload;
    return convertEpochToDate(newData.key, DEFAULT_DATE_FORMAT, true);
  }
  return activeLabel;
};
