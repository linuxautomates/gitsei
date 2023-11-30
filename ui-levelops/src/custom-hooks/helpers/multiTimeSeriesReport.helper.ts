import { forEach } from "lodash";
import { convertEpochToDate, DateFormats } from "utils/dateUtils";
import { getXAxisTimeLabel } from "utils/dateUtils";

const mergeArrayElementsWithSameName = (data: any[]) => {
  let newData: any[] = [];
  forEach(data ?? [], trend => {
    const present = newData.findIndex((nTrend: any) => trend?.name === nTrend?.name);
    if (present !== -1) {
      newData[present] = { ...newData[present], ...(trend || {}) };
    } else {
      newData.push(trend);
    }
  });
  return newData;
};

export const mapMultiTimeSeriesData = (
  data: { [x: string]: { data: any[]; widgetName: string; stackKeys?: string[] } },
  interval: string,
  options?: { weekDateFormat?: string }
) => {
  const barKeys = ["number_of_tickets_closed", "total_tickets"];
  let finalData: any[] = [];
  forEach(Object.keys(data), (wId: string) => {
    const dataSet = data[wId].data;
    const widgetName = data[wId].widgetName;
    const stackKeys = data[wId].stackKeys || [];
    forEach(dataSet, dSet => {
      let newElement: any = {};
      forEach(Object.keys(dSet), dSetKey => {
        const mappedKey = `${dSetKey}^^${widgetName}^^${barKeys.includes(dSetKey) ? "bar" : "line"}`;
        if (["name", "key", "id", "additional_key", "timestamp"].includes(dSetKey)) {
          newElement[dSetKey] = dSet[dSetKey];
        } else if (stackKeys.includes(dSetKey)) {
          newElement[`${dSetKey}^^${widgetName}^^stack`] = dSet[dSetKey];
        } else {
          newElement[mappedKey] = dSet[dSetKey];
        }
      });
      finalData.push(newElement);
    });
  });

  finalData = finalData
    .sort((a: any, b: any) => parseInt(a.timestamp) - parseInt(b.timestamp))
    .map((item: any) => {
      let name = item.name;
      name = getXAxisTimeLabel({ interval, key: item.timestamp, options });
      return { ...item, name };
    });

  return { data: mergeArrayElementsWithSameName(finalData) };
};
