import { forEach } from "lodash";
import { DORA_METRIC_CONFIGURABLE_DEFINITIONS } from "../constants";

export const getDefaultMetricDefinitionState = (metric: string) => {
  let defaultState: any = {};
  forEach(Object.keys(DORA_METRIC_CONFIGURABLE_DEFINITIONS), key => {
    defaultState = {
      ...defaultState,
      [key]: { value: metric, checked: true, key: "$begins" }
    };
  });
  return defaultState;
};
