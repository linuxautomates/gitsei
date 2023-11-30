import { tableCell } from "utils/tableUtils";
import { dateTimeBoundFilterKeys, timeBoundFilterKeys } from "../../graph-filters/components/DateOptionConstants";
import { get } from "lodash";
import { convertEpochToHumanizedForm } from "utils/timeUtils";
import moment from "moment";
import { DateFormats } from "utils/dateUtils";
import { checkTimeSecondOrMiliSecond } from "../helper";

export const csvDrilldownDataTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders, filters } = data;
  let headers = jsxHeaders ?? columns;
  return (apiData || []).map((record: any) => {
    return [...(headers || [])]
      .map((col: any) => {
        if (col.key?.includes("customfield_") || col.key?.includes("Custom.")) {
          const customField: any = get(record, ["custom_fields", col.key], undefined);
          if (Array.isArray(customField)) {
            if (!customField.length) return "";
            return `"${customField.join(",")}"`;
          }

          if (typeof customField === "string") {
            if (customField.includes(",")) {
              return `"${customField}"`;
            }
          }
          return customField;
        }

        if (col.key === "custom_fields_mappings") {
          let test = record[col.key];
          for (let _test of test) {
            let customField: any = get(record, ["custom_fields", _test.key], undefined);
            if (Array.isArray(customField)) {
              if (!customField.length) return "";
              return `"${customField.join(",")}"`;
            }

            if (typeof customField === "string") {
              if (customField.includes(",")) {
                return `"${customField}"`;
              }
            }
          }
        }

        let result = record[col.key];
        if (result === undefined || result === null) {
          return "";
        }
        if (Array.isArray(result)) {
          if (!result.length) return "";
          return `"${result.join(",")}"`;
        }
        if (typeof result === "string") {
          if (result.includes(",")) {
            return `"${result}"`;
          }
          return result;
        }
        if (col.key === "change_time") {
          const timestamp = get(record, [col.key], 0);
          const date = moment(timestamp).utc().format(DateFormats.DAY_TIME);
          if (!timestamp) {
            return "NA";
          }
          return date;
        }

        if (col.key === "resolution_time") {
          const issue_resolved_at = get(record, ["issue_resolved_at"], 0);
          const issue_created_at = get(record, ["issue_created_at"], 0);
          const timestamp = issue_resolved_at - issue_created_at;
          if (issue_resolved_at === 0 || issue_created_at === 0) {
            return "NA";
          }
          return convertEpochToHumanizedForm("days", timestamp);
        }

        if (col.key === "solve_time") {
          const timestamp = get(record, [col.key], 0);
          return timestamp !== 0 ? convertEpochToHumanizedForm("days", timestamp) : "NA";
        }

        if (col.key === "scm_resolution_time") {
          const issue_resolved_at = get(record, ["issue_closed_at"], 0) / 1000;
          const issue_created_at = get(record, ["issue_created_at"], 0) / 1000;
          const timestamp = issue_resolved_at - issue_created_at;
          if (issue_resolved_at === 0 || issue_created_at === 0) {
            return "NA";
          }
          return convertEpochToHumanizedForm("days", timestamp, true);
        }

        if (col.key === "ticket_lifetime") {
          let recordKey = record.hasOwnProperty("workitem_created_at") ? "workitem_created_at" : "issue_created_at";
          const endTimeStamp = get(filters, ["filter", "ingested_at"], undefined);
          const timeStamp = recordKey === "issue_created_at" ? record[recordKey] : record[recordKey] / 1000;
          const startdate = moment.unix(timeStamp).utc().format(DateFormats.DAY);
          const endDate = moment.unix(endTimeStamp).utc().format(DateFormats.DAY);
          const start = moment(startdate, DateFormats.DAY);
          const end = moment(endDate, DateFormats.DAY);
          let days = moment.duration(end.diff(start)).asDays();
          days = Math.round(days);
          return days === 1 ? days + " Day" : days + " Days";
        }

        if(dateTimeBoundFilterKeys.includes(col.key)){
          return tableCell(`time_utc_f2`, result)
        }

        if (
          timeBoundFilterKeys.includes(col.key) ||
          col.key?.includes("created") ||
          col.key?.includes("updated") ||
          col.key?.includes("modify")
        ) {
          if (col.key?.includes("workitem_")) {
            result = checkTimeSecondOrMiliSecond(result);
          }

          if (col.title.includes("UTC")) {
            return new Date(result).toISOString().replace(/T|Z/g, " ");
          }
          return tableCell("created_at", result);
        }

        if (col.key?.includes("customfield_") || col.key?.includes("Custom.")) {
          const customField: any = get(record, ["custom_fields", col.key], undefined);
          if (Array.isArray(customField)) {
            if (!customField.length) return "";
            return `"${customField.join(",")}"`;
          }

          if (typeof customField === "string") {
            if (customField.includes(",")) {
              return `"${customField}"`;
            }
          }
          return customField;
        }

        if(col.key === "velocity_stage_total_time" ){
          return `${(result / 86400).toFixed(1)} days`
        }

        return result;
      })
      .join(",");
  });
};
