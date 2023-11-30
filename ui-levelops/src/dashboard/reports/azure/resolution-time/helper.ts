import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { get } from "lodash";
import { ATTRIBUTE_FILTERS, AZURE_ID_FILTERS } from "../constant";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { WIDGET_ERROR_MESSAGE } from "./constant";

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

export const validateWidget = (args: any) => {
  const metric = get(args, ["query", "metric"], []);
  const sort_xaxis = get(args, ["query", "sort_xaxis"], "");

  if (metric && metric.length > 1 && [widgetDataSortingOptionKeys.VALUE_LOW_HIGH, widgetDataSortingOptionKeys.VALUE_HIGH_LOW].includes(sort_xaxis)) {
    return { saveWidget: false, errorMessage: WIDGET_ERROR_MESSAGE }
  }
  return { saveWidget: true, errorMessage: "" }
};
