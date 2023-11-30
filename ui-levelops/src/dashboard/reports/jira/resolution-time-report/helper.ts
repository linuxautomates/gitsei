import { GROUP_BY_TIME_FILTERS } from "constants/filters";
import { convertEpochToDate, DateFormats, isvalidTimeStamp } from "utils/dateUtils";
import moment from "moment";
import { CustomTimeBasedTypes } from "../../../graph-filters/components/helper";
import { idFilters } from "../commonJiraReports.constants";
import { forEach, get } from "lodash";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { WIDGET_ERROR_MESSAGE } from "./constants";

export const resolutionTimeReportXAxisTransform = (params: any) => {
  const { interval, across, item = {}, CustomFieldType } = params;
  const { key, additional_key } = item;

  let newLabel = key;

  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  const isValidDate = isvalidTimeStamp(newLabel);
  if (
    (CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate && !GROUP_BY_TIME_FILTERS.includes(across)) ||
    (isValidDate && !GROUP_BY_TIME_FILTERS.includes(across))
  ) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (GROUP_BY_TIME_FILTERS.includes(across)) {
    switch (interval) {
      case "week":
      case "day": {
        newLabel = convertEpochToDate(key, DateFormats.DAY, true);
        break;
      }
      case "month": {
        newLabel = convertEpochToDate(key, DateFormats.MONTH, true);
        break;
      }
      case "quarter": {
        newLabel = convertEpochToDate(key, DateFormats.QUARTER_MONTH, true);
        break;
      }
    }
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

export const resolutionTimeOnChartClickPayloadHandler = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if ((across && idFilters.includes(across)) || across?.includes(CUSTOM_FIELD_PREFIX)) {
    keyValue = {
      id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
      name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
    };
  }
  return keyValue;
};

export const resolutionTimeReportChartUnits = (filters: any) => {
  const metric = filters?.filter?.metric || [];
  let isTicket = false;
  let isDay = false;
  let units = [];
  forEach(metric, (item: string) => {
    if (item.includes("tickets")) isTicket = true;
    else isDay = true;
  });
  if (isDay) {
    units.push("Days");
  }
  if (isTicket) {
    units.push("Tickets");
  }

  if (!isDay && !isTicket) {
    units.push("Days", "Tickets");
  }
  return units;
};

export const validateWidget = (args: any) => {
  const metric = get(args, ["query", "metric"], []);
  const sort_xaxis = get(args, ["query", "sort_xaxis"], "");

  if (metric && metric.length > 1 && [widgetDataSortingOptionKeys.VALUE_LOW_HIGH, widgetDataSortingOptionKeys.VALUE_HIGH_LOW].includes(sort_xaxis)) {
    return { saveWidget: false, errorMessage: WIDGET_ERROR_MESSAGE }
  }
  return { saveWidget: true, errorMessage: "" }
};
