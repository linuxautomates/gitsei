import { map } from "lodash";
import { previousTimeStamps } from "utils/timeUtils";

export const updateMockData = (data: any[]) => {
  const newTimeStamps = previousTimeStamps(data.length);
  return map(data, (item: any, index: number) => ({ ...item, key: newTimeStamps[index] }));
};
