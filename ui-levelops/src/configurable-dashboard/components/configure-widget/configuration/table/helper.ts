import { YAxisType } from "configurable-dashboard/components/configure-widget-modal/config-table-widget-modal/ConfigTableWidgetAxis";
import { map, uniqBy } from "lodash";

export const uniqYAxisWithOrderPreserved = (yaxis: YAxisType[]) => {
  const replaceEmptyKeyWith = "NO_KEY";
  const mappedYAxis: YAxisType[] = map(yaxis || [], (axis: YAxisType, index: number) => {
    if (!axis?.key) {
      return {
        ...(axis || {}),
        key: `${replaceEmptyKeyWith}-${index}`
      };
    }
    return axis;
  });
  const uniqMappedYAxis = uniqBy(mappedYAxis, "key");
  return map(uniqMappedYAxis || [], yaxis => {
    if (yaxis?.key?.includes(replaceEmptyKeyWith)) {
      return {
        ...(yaxis || {}),
        key: ""
      };
    }
    return yaxis;
  });
};
