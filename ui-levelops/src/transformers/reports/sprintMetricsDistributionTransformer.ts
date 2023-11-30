import { uniq } from "lodash";
import { findSumInArray } from "utils/commonUtils";

const groupBy = (arr: Array<any>, key: any, avgKey: any) => {
  const res = [] as any;
  let grouped = {} as any;
  const newArr = JSON.parse(JSON.stringify(arr));
  newArr.forEach((val: any) => {
    if (grouped[val[key]]) {
      grouped[val[key]].count = grouped[val[key]].count + 1;
      grouped[val[key]].planned = grouped[val[key]].planned + val.planned;
      grouped[val[key]].unplanned = grouped[val[key]].unplanned + val.unplanned;
      grouped[val[key]].delivered_keys = grouped[val[key]].delivered_keys.concat(val.delivered_keys);
      if (avgKey === "delivered_tickets") {
        grouped[val[key]][avgKey] = grouped[val[key]].delivered_tickets + val.planned + val.unplanned;
      } else {
        grouped[val[key]][avgKey] = grouped[val[key]][avgKey] + val[avgKey];
      }
    } else {
      grouped[val[key]] = val;
      grouped[val[key]].name = val[key] + "%";
      grouped[val[key]].count = 1;
      if (avgKey === "delivered_tickets") {
        delete val.delivered_story_points;
        grouped[val[key]].delivered_tickets = val.planned + val.unplanned;
      }
    }
  });
  Object.keys(grouped).forEach(val => {
    grouped[val].planned = Math.round(grouped[val].planned / grouped[val].count);
    grouped[val].unplanned = Math.round(grouped[val].unplanned / grouped[val].count);
    grouped[val][avgKey] = Math.round(grouped[val][avgKey] / grouped[val].count);
    grouped[val].delivered_keys = uniq(grouped[val].delivered_keys);
    res.push(grouped[val]);
  });

  return res;
};

const getPercent = (arr: any, key: any, totalPercent: any) => {
  return arr.map((val: any) => {
    val.percent = Math.round((val[key] / totalPercent) * 100);
    val.title = `Avg of ${key} ${val[key]} (${val.percent}%)`;
    return val;
  });
};

export const sprintMetricsDistributionTransformer = (data: any) => {
  let transformedData = [] as any;
  const { apiData, filters } = data;
  const mappings = {
    ticket_count: "delivered_tickets",
    story_points: "delivered_story_points"
  } as any;
  if (apiData && apiData?.length > 0) {
    transformedData = groupBy(apiData, "key", mappings[filters?.agg_metric]);
    const totalTime = findSumInArray(transformedData, mappings[filters?.agg_metric]);
    transformedData = getPercent(transformedData, mappings[filters?.agg_metric], totalTime);
  }
  return { data: transformedData };
};
