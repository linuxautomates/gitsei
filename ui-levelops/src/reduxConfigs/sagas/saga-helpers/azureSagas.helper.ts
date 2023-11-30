import { get, map } from "lodash";

// Use this to normalize the azure apidata records
export const getNormalizedFiltersData = (records: any[]) => {
  return map(records || [], record => {
    const key = Object.keys(record)[0];
    const optionsRecords = get(record[key] || {}, ["records"], []);
    return {
      [key]: optionsRecords
    };
  });
};
