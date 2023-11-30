import moment from "moment";
import { DATE_RANGE_FILTER_FORMAT } from "../../../../shared-resources/containers/server-paginated-table/containers/filters/constants";
import { capitalize } from "lodash";
import { TriageFilterResponse } from "reduxConfigs/actions/restapi/response-types/triageResponseTypes";

export const sanitizeTimeFilter = (data: TriageFilterResponse) => {
  if (data?.filter?.start_time || data?.filter?.end_time) {
    let start_time = "";
    let end_time = "";
    const _timeData = [data.filter.start_time, data.filter.end_time].map((filter: any) => {
      if (!Array.isArray(filter)) {
        if (typeof filter === "object" && Object.keys(filter).length > 0) {
          // expected to have { $gt: "", $lt: "" }
          if (filter?.$gt && filter?.$gt?.length >= 10) {
            start_time = moment.unix(parseInt(filter.$gt)).utc().format(DATE_RANGE_FILTER_FORMAT);
          }

          if (filter.$lt && filter?.$lt?.length >= 10) {
            end_time = moment.unix(parseInt(filter.$lt)).utc().format(DATE_RANGE_FILTER_FORMAT);
          }

          return "";
        }
      }

      return filter;
    });
    if (start_time || _timeData[0]) {
      data.filter.start_time = start_time || _timeData[0];
    } else {
      delete data.filter.start_time;
    }

    if (end_time || _timeData[1]) {
      data.filter.end_time = end_time || _timeData[1];
    } else {
      delete data.filter.end_time;
    }
  }
  return data;
};

export const getTriageFilterNameFromId = (id?: string) =>
  id
    ?.split("-")
    .map(word => capitalize(word))
    .join(" ") || "";

export const checkArrayKeys = (arr1: any, arr2: any) => {
  return arr2.find((val: string) => {
    return arr1.includes(val);
  });
};
