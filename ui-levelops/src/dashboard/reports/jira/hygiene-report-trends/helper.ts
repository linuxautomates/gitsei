import { get } from "lodash";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

export const hygieneTrendOnChartClickPayload = (params: any) => {
  if (params?.visualization === ChartType?.HYGIENE_BAR_CHART) {
    return {
      id: get(params, ["data", "key"]),
      name: get(params, ["data", "name"]),
      hygiene: params?.hygiene
    };
  } else {
    return {
      id: get(params, ["data", "activePayload", 0, "payload", "key"]),
      name: get(params, ["data", "activePayload", 0, "payload", "name"]),
      hygiene: params?.hygiene
    };
  }
};
