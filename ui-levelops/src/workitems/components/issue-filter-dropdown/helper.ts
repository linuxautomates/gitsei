import moment from "moment";
import { capitalize } from "lodash";
import { DATE_RANGE_FILTER_FORMAT } from "shared-resources/containers/server-paginated-table/containers/filters/constants";

export const sanitizeTimeFilter = (data: any) => {
  if (data?.filter?.updated_at || data?.filter?.updated_end) {
    let start_time = "";
    let end_time = "";
    const _timeData = [data.filter.updated_at, data.filter.updated_end].map((filter: any) => {
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
      data.filter.updated_at = start_time || _timeData[0];
    } else {
      delete data.filter.updated_at;
    }

    if (end_time || _timeData[1]) {
      data.filter.updated_end = end_time || _timeData[1];
    } else {
      delete data.filter.updated_end;
    }
  }
  return data;
};

export const getIssueFilterNameFromId = (id?: string) =>
  id
    ?.split("-")
    .map(word => capitalize(word))
    .join(" ") || "";
