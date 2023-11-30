import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";
import { convertToDay, dynamicColumnPrefix, getTimeAndIndicator } from "custom-hooks/helpers/leadTime.helper";
import { forEach, get, uniqBy } from "lodash";
import moment from "moment";

export const leadTimeByTimeSpentInStagesCsvTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders } = data;

  const columnsArr = (columns || []).map((data: { title: string; key: string; }) => {return { title: data.title, key: data.key }});
  
  return (apiData || []).map((record: any) => {
    const defaultColumnData = uniqBy([...(jsxHeaders || []), ...(columnsArr || [])],"key").map((col: any) => {
      let result = record[col.key];
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
      if(["additional_key"].includes(col?.key)){
        return result ||  record?.key;
      }
      if (["total"].includes(col?.key) || (col?.key).includes(dynamicColumnPrefix)) {
        const lower_limit_unit = get(record, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
        const lower_limit_value = get(record, ["velocity_stage_result", "lower_limit_value"], 0);
        const upper_limit_unit = get(record, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
        const upper_limit_value = get(record, ["velocity_stage_result", "upper_limit_value"], 0);
        const lower_limit = convertToDay(lower_limit_value, lower_limit_unit);
        const upper_limit = convertToDay(upper_limit_value, upper_limit_unit);
        let totalTime = 0;
        // CHECK IF KEY EXISTS ONLY USE IN THIS WIDGET LEAD TIME BY TIME SPENT IN STAGES
        const velocityStageKey = record.hasOwnProperty("velocity_stage_total_time");
        if (velocityStageKey) {
          totalTime = record?.velocity_stage_total_time ?? 0;
        } else {
          forEach(get(record, ["velocity_stages"], []), (stage: { time_spent: number }) => {
            totalTime += stage?.time_spent ?? 0;
          });
        }
        if (col?.key !== "total") {
          const updatedKey = col?.key.replace(dynamicColumnPrefix, "");
          const stage = (record?.velocity_stages || []).find((item: any) => item?.stage === updatedKey);
          if (stage) {
            totalTime = get(stage, ["time_spent"], 0);
          }
        }
        totalTime = convertToDay(totalTime, AcceptanceTimeUnit.SECONDS);
        const time = getTimeAndIndicator(totalTime, lower_limit, upper_limit);
        return `${time.duration} ${time.unit}`;
      }

      return result;
    });

    return [...(defaultColumnData || [])].join(",");
  });

};
