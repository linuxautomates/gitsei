import { getWidgetConstant } from "../../../constants/widgetConstants";
import { get, unset } from "lodash";
import { DEFAULT_MAX_RECORDS, valuesToFilters } from "../../../constants/constants";
import widgetConstants from '../../../constants/widgetConstants'
import { ALLOWED_WIDGET_DATA_SORTING } from "../../../constants/filter-name.mapping";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { convertToDays } from "utils/timeUtils";

export const timeToResolveTransformer = (data: any) => {
  const { records, sortBy, reportType, metadata, filters: widgetFilters, isMultiTimeSeriesReport } = data;
  let { apiData } = data;
  const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
  const leftYAxis = get(metadata, "leftYAxis", "mean");
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], "day");
  apiData = (apiData || []).map((item: any) => {
    const { stacks } = item;

    let stackData = {};
    if (stacks) {
      let stackedTicketsTotal = 0;
      let stackedTicketsOtherTotal = stacks.slice(10, stacks.length).reduce((acc: number, obj: any) => {
        acc = acc + obj["count"];
        return acc;
      }, 0);

      stackData = stacks
        .sort((a: any, b: any) => b["count"] - a["count"])
        .slice(0, 10)
        .reduce((acc: any, obj: any) => {
          // if key = "" then replace it with UNKNOWN
          acc[xAxisLabelTransform?.({ item: obj, interval, across }) || "UNKNOWN"] = obj["count"];
          stackedTicketsTotal += obj["count"];
          return acc;
        }, {});
      const missingTickets =
        stackedTicketsOtherTotal + Math.max(item["count"] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);
      if (missingTickets > 0) {
        stackData = {
          ...stackData,
          Other: missingTickets
        };
      }
    }

    const name = xAxisLabelTransform?.({ item, interval, across });

    const mappedItem: any = {
      count: item?.count,
      [leftYAxis]: convertToDays(item?.[leftYAxis] || 0),
      ...stackData,
      name,
      key: item.key
    };
    if (isMultiTimeSeriesReport) {
      mappedItem["timestamp"] = item.key;
    }
    return mappedItem;
  });

  // sorting X-axis
  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  if (allowedWidgetDataSorting) {
    const sortValue = get(widgetFilters, ["sort_xaxis"], "");
    sortValue.includes("old-latest") && apiData.reverse();
  }

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, apiData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length - maxRecords : 0;
  const slice_end = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  apiData = apiData.slice(slice_start, slice_end);
  const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
  const shouldReverseApiData = getShouldReverseApiData?.(sortBy);
  if (shouldReverseApiData) {
    apiData.reverse();
  }

  return {
    data: apiData
  };
};

export const timeToResolveDrilldownTransform = (data: any) => {
  const { drillDownProps, widget } = data;
  let { acrossValue, filters } = genericDrilldownTransformer(data);

  const x_axis = get(drillDownProps, ["x_axis"], "");
  const filterValue = typeof x_axis === "object" ? x_axis?.id : x_axis;

  if (filterValue === "NA") {
    const widgetConstantFilterValue = get(
      widgetConstants,
      [widget?.type || "", "valuesToFilters", acrossValue],
      undefined
    );
    const filterKey = widgetConstantFilterValue || get(valuesToFilters, [acrossValue], acrossValue);
    unset(filters, ["filter", filterKey]);

    filters = {
      ...filters,
      filter: {
        ...filters?.filter,
        missing_fields: {
          [acrossValue]: true
        }
      }
    };
  }

  return { acrossValue, filters };
};
