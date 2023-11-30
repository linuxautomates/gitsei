import moment from "moment";
import { get } from "lodash";
import { isvalidTimeStamp, getXAxisTimeLabel, unixUTCToDate, DateFormats } from "utils/dateUtils";
import { TIME_FILTERS_KEYS, ID_FILTERS } from "../../../constants/filters";
import { AZURE_FILTER_KEYS } from "dashboard/reports/azure/constant";
// import { STATIC_PRIORTIES} from "../../charts/helper"
import { STATIC_PRIORTIES } from "../../charts/jira-prioirty-chart/helper";

// TODO : replace idFilters with ID_FILTERS
// const idFilters = ["assignee", "reporter", "first_assignee"];

// TODO: Move to constant folder
// export const CustomTimeBasedTypes = ["date", "datetime", "dateTime"];
const CustomTimeBasedTypes = ["date", "datetime", "dateTime"];

export const getXAxisLabel = (props: Record<string, any>) => {
  const { interval, across, item = {}, weekDateFormat = "", CustomFieldType } = props;
  const { key, additional_key } = item;
  let newLabel = key;

  if (["fix_version"].includes(across)) {
    return newLabel;
  }
  const isValidDate = isvalidTimeStamp(newLabel);
  if (ID_FILTERS.includes(across)) {
    newLabel = additional_key || key;
    return newLabel;
  }
  if (
    (CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) ||
    (isValidDate && !TIME_FILTERS_KEYS.includes(across))
  ) {
    newLabel = unixUTCToDate(key,DateFormats.DAY);
    return newLabel;
  }
  // azure
  if (["priority"].includes(across)) {
    newLabel = get(STATIC_PRIORTIES, [key], key);
  }
  if (TIME_FILTERS_KEYS.includes(across)) {
    newLabel = getXAxisTimeLabel({ interval, key, options: { weekDateFormat } });
  }
  // azure
  if (across === AZURE_FILTER_KEYS.CODE_AREA) {
    newLabel = key.split("\\").pop() || key;
  }
  // azure
  // if (across === "trend") {
  //   newLabel = convertEpochToDate(key, DateFormats.DAY, true);
  // }

  if (!newLabel || newLabel === "NA") {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

/**
 * Transforming the stack filter list to back-end understandable keys.
 * As of now, for labels we are using additional_key to display on legend and key as filter payload.
 */
export const generateStackFilters = (apiData: any, filters: any, id?: string) => {
  const selectedFilteredList: string[] = [];

  if (apiData?.[id!]) {
    const data = apiData[id!];
    data?.forEach((element: { [key: string]: any; stacks: { additional_key?: string; key: string }[] }) => {
      element?.stacks?.forEach(({ key, additional_key }) => {
        const filterKey = additional_key ?? key;

        if (!!filters?.[filterKey]) {
          const usedKey = key ?? additional_key;
          if (["string", "number"].includes(typeof usedKey) && !selectedFilteredList.includes(usedKey)) {
            selectedFilteredList.push(usedKey);
          }
        }
      });
    });
  }

  return selectedFilteredList;
};
