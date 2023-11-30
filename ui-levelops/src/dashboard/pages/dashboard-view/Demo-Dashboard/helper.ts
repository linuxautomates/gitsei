import { cloneDeep, forEach, get, set } from "lodash";
import { BaseReportTypes } from "model/report/baseReport.constant";
import { CONFIG_KEY_TO_DEMO_KEY_MAPPING } from "./constant";
import { DemoWidgetDataType } from "./types";

/** This function transforms widget config as per it's demo widget settings */
export const transformDemoWidgetConfig = (widgetConfig: BaseReportTypes, widgetData: DemoWidgetDataType) => {
  const nWidgetConfg = cloneDeep(widgetConfig);
  const keysToSet: string[] = ["name", "description"];

  forEach(keysToSet, key => {
    set(nWidgetConfg, [key], get(widgetData, [CONFIG_KEY_TO_DEMO_KEY_MAPPING[key]]));
  });

  return nWidgetConfg;
};
