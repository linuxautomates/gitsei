import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { DateFormats, convertEpochToDate } from "utils/dateUtils";
import { convertToDays, convertToMins } from "utils/timeUtils";
import { AcceptanceTimeUnit } from "classes/RestWorkflowProfile";
import { convertToDay } from "./leadTime.helper";
import { v1 as uuid } from "uuid";

export const leadTimeTrendTransformer = (data: any) => {
  const { reportType, apiData, metadata, widgetFilters } = data;
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const dataKey = get(metadata, "metrics", "mean");
  let dateFormat = DateFormats.DAY;
  const trendsData = (apiData || []).map((item: any) => {
    const { key, data: stages } = item;
    const stageData = (stages || []).reduce(
      (acc: any, i: any) => ({
        ...acc,
        [i.key]: convertTo === "days" ? convertToDays(i[dataKey]) : convertToMins(i[dataKey])
      }),
      {}
    );

    return {
      key,
      name: convertEpochToDate(key, dateFormat, true),
      ...stageData
    };
  });

  return { data: trendsData };
};

export const leadTimePhaseTransformer = (data: any) => {
  const { reportType, apiData, metadata } = data;
  const dataKey = get(metadata, "metrics", "mean");
  const phases = (apiData || []).map((item: any) => {
    const { key } = item;
    const lower_limit_unit = get(item, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
    const lower_limit_value = get(item, ["velocity_stage_result", "lower_limit_value"], 0);
    const upper_limit_unit = get(item, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
    const upper_limit_value = get(item, ["velocity_stage_result", "upper_limit_value"], 0);
    const isDataInMiliSecond = get(widgetConstants, [reportType, "isDataInMiliSecond"], false);
    const rating = get(item, ["velocity_stage_result", "rating"], "");
    return {
      id: uuid(),
      name: key,
      event_name: item.additional_key,
      lower_limit: convertToDay(lower_limit_value, lower_limit_unit),
      upper_limit: convertToDay(upper_limit_value, upper_limit_unit),
      duration: item[dataKey] / (isDataInMiliSecond ? 86400000 : 86400),
      rating,
      info_message: key === 'Other' ?
        `Unmapped statuses will be categorized as 'OTHER.'`
        : ''
    };
  });

  return { data: phases };
};

export const leadTimeTypeTransformer = (data: any) => {
  const { reportType, apiData, metadata } = data;
  const dataKey = get(metadata, "metrics", "mean");
  const trendsData = (apiData || []).map((item: any) => {
    const { key, stacks } = item;

    const stageData = (stacks || []).reduce((acc: any, item: any) => {
      const lower_limit_unit = get(item, ["velocity_stage_result", "lower_limit_unit"], AcceptanceTimeUnit.DAYS);
      const lower_limit_value = get(item, ["velocity_stage_result", "lower_limit_value"], 0);
      const upper_limit_unit = get(item, ["velocity_stage_result", "upper_limit_unit"], AcceptanceTimeUnit.DAYS);
      const upper_limit_value = get(item, ["velocity_stage_result", "upper_limit_value"], AcceptanceTimeUnit.DAYS);
      const isDataInMiliSecond = get(widgetConstants, [reportType, "isDataInMiliSecond"], false);
      return {
        ...acc,
        [item.key]: {
          duration: item[dataKey] / (isDataInMiliSecond ? 86400000 : 86400),
          count: item.count,
          lower_limit: convertToDay(lower_limit_value, lower_limit_unit),
          upper_limit: convertToDay(upper_limit_value, upper_limit_unit)
        }
      };
    }, {});

    return {
      name: key,
      ...stageData
    };
  });

  return { data: trendsData };
};
