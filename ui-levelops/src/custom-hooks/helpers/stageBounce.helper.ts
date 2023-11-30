import { get } from "lodash";
import { DEFAULT_MAX_RECORDS } from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from 'dashboard/constants/widgetConstants'

export const stageBounceDataTransformer = (data: any) => {
  const { records, sortBy, reportType, filters: widgetFilters } = data;
  let { apiData } = data;
  const across = get(widgetFilters, ["across"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  const metric = get(widgetFilters, ["metric"], "mean");
  const interval = get(widgetFilters, ["interval"], undefined);
  const xAxisIgnoreSortKeys = get(widgetConstants, [reportType, "chart_props", "xAxisIgnoreSortKeys"], []);
  const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  const mappedData = (apiData || []).reduce((acc: any, next: any) => {
    const key = next.key || "UNKNOWN";
    let item = acc?.[key] || {};
    if ((next?.stacks || []).length) {
      const stackData = (next?.stacks || []).reduce((obj: any, nxt: any) => {
        const stackKey = `${next.stage} - ${nxt.additional_key || nxt.key}`;
        return {
          ...obj,
          [stackKey]: nxt?.[metric]
        };
      }, {});
      item = {
        ...item,
        ...stackData
      };
    } else {
      item = {
        ...item,
        [next.stage]: next?.[metric]
      };
    }

    return {
      ...acc,
      [key]: {
        ...item,
        key,
        name: xAxisLabelTransform?.({ item: next, interval, across }) || key
      }
    };
  }, {});

  let seriesData = Object.values(mappedData);

  const maxRecords = Math.min(records || DEFAULT_MAX_RECORDS, seriesData.length);

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ reportType });
  let slice_start = seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length - maxRecords : 0;
  const slice_end =
    seriesData?.length > 0 && shouldSliceFromEnd ? seriesData.length : maxRecords || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  let getSortFromConstant = getWidgetConstant(reportType, ["getSortKey"]);
  let _sortBy = getSortFromConstant?.({}) || sortBy;

  if (!xAxisIgnoreSortKeys.includes(across)) {
    const _sortOrder = getWidgetConstant(reportType, ["getSortOrder"]) || "desc";
    if (_sortOrder === "desc") {
      seriesData = seriesData.sort((a: any, b: any) => (b[_sortBy] || 0) - (a[_sortBy] || 0));
    } else {
      seriesData = seriesData.sort((a: any, b: any) => (a[_sortBy] || 0) - (b[_sortBy] || 0));
    }
  }

  seriesData = seriesData.slice(slice_start, slice_end);
  const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
  const shouldReverseApiData = getShouldReverseApiData?.({});
  if (shouldReverseApiData) {
    seriesData.reverse();
  }

  return {
    data: seriesData
  };
};
