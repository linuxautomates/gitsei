import { DevRawStatsDataTypeNew } from "dashboard/reports/dev-productivity/individual-raw-stats-report/types";
import { rawStatsDaysColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { cloneDeep, get, upperCase } from "lodash";
import { convertToDays } from "utils/timeUtils";
import { v1 as uuid } from "uuid";

export const doraLeadTimeForChangeTransformer = (response: any) => {
  const stages = get(response, ["stages"], []);
  const updatedStages = stages.map((stage: any) => {
    const id = uuid();
    const pieKeys = ["good_count", "acceptable_count", "slow_count"];
    const pieData = pieKeys.reduce((acc: any, next: string) => {
      const rating = upperCase(next.replace("_count", ""));
      return [...acc, { count: stage[next] || 0, rating: rating }];
    }, []);
    return {
      ...stage,
      id: id,
      pieData: pieData
    };
  });
  return {
    ...response,
    stages: updatedStages
  };
};

export const transformData = (data: Array<DevRawStatsDataTypeNew>) =>
  (data ?? [])
    ?.map((newData: DevRawStatsDataTypeNew) => {
      const section_responses = cloneDeep(newData?.section_responses);
      const valuesObj: any = {};
      section_responses?.filter((section: any) => {
        section?.feature_responses?.filter((feature: any) => {
          if (feature?.count !== undefined) {
            valuesObj[feature?.name] = feature?.count;
            valuesObj[`${feature?.name}_color`] = feature?.rating;
          }
        });
      });
      delete newData.section_responses;
      return { ...newData, ...valuesObj, ...(newData?.custom_fields || {}) };
    })
    .map((data: any) => {
      const newObject = {};
      const keys = Object.keys(data);
      keys.forEach((key: string) => {
        let value = data[key];
        if (rawStatsDaysColumns.includes(key)) {
          value = convertToDays(value);
        }
        // @ts-ignore
        newObject[key] = value;
      });
      return newObject;
    });

export const getRawStatRatingAndCount = (arr: Array<Record<any, any>>) => {
  const valuesObj: any = {};
  (arr || [])?.forEach((val: any) => {
    if (val?.count !== undefined) {
      valuesObj[val.name] = val.count;
      valuesObj[val.name + "_color"] = val?.rating;
    }
  });
  return valuesObj;
};