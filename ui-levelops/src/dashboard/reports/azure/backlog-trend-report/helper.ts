import { get } from "lodash";

export const OnChartClickPayload = (param: any) => {
  const timeStamp = get(param, ["data", "activePayload", 0, "payload", "key"], undefined);
  const label = get(param, ["data", "activeLabel"], undefined);
  return { id: timeStamp, name: label };
};
