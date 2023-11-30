import { random } from "lodash";
import { convertNumArrayToStringArray } from "./arrayUtils";
import { previousTimeStamps } from "./timeUtils";

export const getMockTrendData = (size: number, key: string = "total") => {
  let data: any[] = [];
  const randomTimeStamps = convertNumArrayToStringArray(previousTimeStamps(size));
  randomTimeStamps.forEach((time: string) => {
    data.push({ key: time, [key]: random(0, 100) });
  });
  return data;
};
