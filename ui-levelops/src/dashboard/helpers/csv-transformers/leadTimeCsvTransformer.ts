import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";
import { convertToDay, getTimeAndIndicator } from "custom-hooks/helpers/leadTime.helper";
import { get } from "lodash";
import { makeCSVSafeString } from "utils/stringUtils";
import { convertEpochToHumanizedForm } from "../../../utils/timeUtils";

export const leadTimeCsvTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders, filters } = data;

  let headersColumns: any[] = [];
  let stageColumns: any[] = [];
  let allStages: any[] = get(apiData, ["0", "data"], []);
  
  jsxHeaders.forEach((header: any) => {
    const stageColumn = allStages.find(stage => header?.key.includes(stage?.key));
    if (stageColumn) {
      stageColumns.push(stageColumn);
    } else {
      headersColumns.push(header);
    }
  });

  return (apiData || []).map((record: any) => {
    const headerColumnsData = [...(headersColumns || [])].map((col: any) => {
      let result = record[col.key];
      if (Array.isArray(result)) {
        if (!result.length) return "";
        return `"${result.join(",")}"`;
      }
      if (typeof result === "string") {
        if (result.includes(",")) {
          return makeCSVSafeString(result);
        }
        return result;
      }

      if (col.key === "total") {
        const data = record.data;
        let lower_limit = 0,
          upper_limit = 0,
          total_lead_time = 0;
        (data || []).forEach((stage: any) => {
          const lower_limit_unit = get(stage, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
          const lower_limit_value = get(stage, ["velocity_stage_result", "lower_limit_value"], 0);
          const upper_limit_unit = get(stage, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
          const upper_limit_value = get(stage, ["velocity_stage_result", "upper_limit_value"], 0);
          total_lead_time += convertToDay(get(stage, ["mean"], 0), AcceptanceTimeUnit.SECONDS);
          lower_limit += convertToDay(lower_limit_value, lower_limit_unit);
          upper_limit += convertToDay(upper_limit_value, upper_limit_unit);
        });
        const time = getTimeAndIndicator(
          record.data.total ? convertToDay(record.data.total, AcceptanceTimeUnit.SECONDS) : total_lead_time,
          lower_limit,
          upper_limit
        );
        return `${time.duration} ${time.unit}`;
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
      return result;
    });

    const allStages = stageColumns.map((stage: any) => {
      const result = (record?.data || []).find((item: any) => item?.key === stage.key);
      if (result?.mean) {
        const lower_limit = convertToDay(
          get(result, ["velocity_stage_result", "lower_limit_value"], 0),
          get(result, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.SECONDS)
        );
        const upper_limit = convertToDay(
          get(result, ["velocity_stage_result", "upper_limit_value"], 0),
          get(result, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.SECONDS)
        );
        const time = getTimeAndIndicator(
          convertToDay(get(result, ["mean"], 0), AcceptanceTimeUnit.SECONDS),
          lower_limit,
          upper_limit
        );
        return `${time.duration} ${time.unit}`;
      }
      return "-";
    });
    return [...(headerColumnsData || []), ...(allStages || [])].join(",");
  });
};
