import { mapColumnsWithRowsById } from "configuration-tables/helper";
import { DEFAULT_MAX_RECORDS, DEFAULT_MAX_STACKED_ENTRIES } from "dashboard/constants/constants";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { genericSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { countBy, get, pick, intersection, isArray, isObject, cloneDeep, forEach, map, keys } from "lodash";
import { lineChartColors } from "shared-resources/charts";
import { COLOR_MAPPING, TREND_LINE_DIFFERENTIATER, UNDEFINED } from "shared-resources/charts/constant";
import { objectSumByKey } from "utils/commonUtils";
import { TableYAxisType } from "./typings";

/**
 * It takes an array of objects, and two keys, and returns an object with the slope, yStart, and a
 * function to calculate the y value for a given x value
 * @param {any[]} data - any[] - the data you want to calculate the trendline for
 * @param {string} xKey - The key of the x-axis data in the data array
 * @param {string} yKey - The key of the data that you want to calculate the trend for.
 * @returns An object with three properties: slope, yStart, and calcY.
 */
export const tableChartTrendDataCalcHelper = (data: any[], xKey: string = "x", yKey: string = "y") => {
  const xData = data.map(value => value[xKey]);
  const yData = data.map(value => value[yKey]);
  function getAvg(arr: any[]) {
    const total = arr.reduce((acc, c) => acc + c, 0);
    return total / arr.length;
  }

  function getSum(arr: any[]) {
    return arr.reduce((acc, c) => acc + c, 0);
  }
  // average of X values and Y values
  const xMean = getAvg(xData);
  const yMean = getAvg(yData);

  // Subtract X or Y mean from corresponding axis value
  const xMinusxMean = xData.map(val => val - xMean);
  const yMinusyMean = yData.map(val => val - yMean);

  const xMinusxMeanSq = xMinusxMean.map(val => Math.pow(val, 2));

  const xy = [];
  for (let x = 0; x < data.length; x++) {
    xy.push(xMinusxMean[x] * yMinusyMean[x]);
  }

  // const xy = xMinusxMean.map((val, index) => val * yMinusyMean[index]);

  const xySum = getSum(xy);

  // b1 is the slope
  const b1 = xySum / getSum(xMinusxMeanSq);
  // b0 is the start of the slope on the Y axis
  const b0 = yMean - b1 * xMean;

  return {
    slope: b1,
    yStart: b0,
    calcY: (x: number) => b0 + b1 * x
  };
};

export const getBaselineMappings = (data: any, rows: any, columns: any) => {
  let baseLineMap: any = {};
  const baseLineValTemp = getFilteredRows({ [data?.ouColumn?.id]: data?.ouIds }, rows, columns);
  const baseLineColumns = data.columns.filter((c: any) => c?.inputType === "base_line");
  const baseLineVal =
    baseLineValTemp?.length > 1 ? baseLineValTemp?.[baseLineValTemp?.length - 1] : baseLineValTemp?.[0];
  baseLineColumns.map((a: any) => {
    const title = a?.title || "";
    const value = title.includes("_low")
      ? COLOR_MAPPING?.low
      : title.includes("_medium")
      ? COLOR_MAPPING?.mid
      : title.includes("_high")
      ? COLOR_MAPPING?.high
      : undefined;
    baseLineMap[baseLineVal?.[a?.dataIndex] || ""] = value;
  });
  return baseLineMap;
};

export const getBaselineValues = (showBaseLines: boolean, baseLineMap: any) => {
  return showBaseLines
    ? keys(baseLineMap)
        .sort((a: any, b: any) => a - b)
        ?.map(Number)
    : [];
};

export const configWidgetDataTransform = (data: any, chartProps: any) => {
  const xAxis = get(data, ["xAxis"], "");
  const yAxis = get(data, ["yAxis"], []);
  const xAxisSort = get(data, ["xAxisSort"]);
  const stackBy = get(data, ["stackBy"]);
  const groupBy = get(data, ["groupBy"]);
  const showBaseLines = get(data, ["showBaseLines"]);
  const columns = get(data, ["columns"], []);
  const yUnit = get(data, ["yUnit"], "");
  const rows = get(data, ["rows"], []);
  const filters = get(data, ["filters"], {});
  const maxRecords = get(data, ["maxRecords"], DEFAULT_MAX_RECORDS);
  const mappedRowsColumns = mapColumnsWithRowsById(rows, columns);
  const xColumn = mappedRowsColumns.find(column => column.id === xAxis);
  let transformData: any[] = [];
  let trendLineData: Record<string, string> = {};
  let yAxisMap: { [key: string]: any } = {};
  let areaProps: any[] = [];
  let yDisplayColors: { [x: string]: string } = {};
  let dataMax = Number.NEGATIVE_INFINITY;
  let dataMin = Number.POSITIVE_INFINITY;
  let baseLineMap: any = {};

  if (xColumn) {
    const xKeys = getXKeys(filters, xColumn, mappedRowsColumns);
    transformData = xKeys.slice(0, maxRecords).map((value: string) => {
      let suffix = value.split("row_")[1].split("_")[0];
      let yAxisData: any = {};
      yAxis.forEach((y: TableYAxisType) => {
        const yColumn = mappedRowsColumns.find(yCol => yCol.id === y.key);
        if (yColumn) {
          const key = Object.keys(yColumn.values || {}).find(yKey => yKey.includes(`row_${suffix}`));
          if (key) {
            let newKey = `${y.key}_${y.value}`;
            if (y.label && y.label.length) {
              newKey = `${y.label.replace(/ /g, "_")}_${y.value}`;
            }
            yDisplayColors = { [newKey]: y.display_color, ...yDisplayColors };
            yAxisMap[newKey] = { ...y, dataKey: newKey };
            const value = parseFloat(get(yColumn, ["values", key], ""));
            let yColumnFilter = get(filters, [y.key]);
            if (value && !isNaN(value)) {
              let formattedValue = y.format === "integer" ? Math.round(value) : value;
              if (formattedValue > dataMax) {
                // Keep track of max value in data.
                dataMax = formattedValue;
              }
              if (formattedValue < dataMin) {
                // Keep track of min value in data.
                dataMin = formattedValue;
              }
              if (!!yColumnFilter) {
                if (typeof yColumnFilter in ["string", "number"]) {
                  const yfilter = parseFloat(yColumnFilter);
                  if (!isNaN(yfilter) && yfilter === value) {
                    yAxisData = {
                      ...yAxisData,
                      [newKey]: formattedValue ?? 0
                    };
                  }
                } else if (Array.isArray(yColumnFilter)) {
                  yColumnFilter = yColumnFilter.map(v => parseFloat(v)).filter(v => !isNaN(v));
                  if (yColumnFilter.includes(value)) {
                    yAxisData = {
                      ...yAxisData,
                      [newKey]: formattedValue ?? 0
                    };
                  }
                }
              } else if (!yColumnFilter || isNaN(yColumnFilter)) {
                yAxisData = {
                  ...yAxisData,
                  [newKey]: formattedValue ?? 0
                };
              }
            }
          }
        }
      });
      return {
        name: get(xColumn, ["values", value], ""),
        ...yAxisData
      };
    });
  }

  const transformer = (value: any, _dataKey: any) => {
    let dataKey;
    let formattedValue = value;
    try {
      // Might not be a string according to composite-chart.component.tsx
      dataKey = _dataKey.toString();
    } catch (e) {
      dataKey = undefined;
    }

    if (dataKey && !isNaN(value)) {
      switch (yAxisMap?.[dataKey]?.format) {
        case "decimal": {
          formattedValue = value.toFixed(2);
          break;
        }
        case "integer": {
          formattedValue = Math.round(value);
          break;
        }
      }
    }

    return formattedValue;
  };

  Object.keys(yAxisMap).forEach(yKey => {
    const yAxis: TableYAxisType & { dataKey: string } = yAxisMap[yKey];
    areaProps.push({ ...yAxis, transformer });
  });

  // handle sorting if xaxis sort is selected
  if (xAxisSort) {
    if (stackBy && stackBy?.length > 0) {
      transformData = stackedSorting(transformData, xAxisSort, "value", xColumn?.type) ?? [];
    } else {
      let dataKey = groupBy ? chartProps?.barProps?.[0]?.dataKey : areaProps?.[0]?.dataKey;
      transformData = xAxisSorting(transformData, xAxisSort, dataKey, xColumn?.type) ?? [];
    }
  }

  Object.keys(yAxisMap).forEach(yKey => {
    const yAxis: TableYAxisType & { dataKey: string } = yAxisMap[yKey];
    /** Calculating trend line data for y-axises where show trend line is true */
    if (yAxis.show_trend_line) {
      const trendLineRawData = transformData.map((data: Record<string, string | number>, index: number) => {
        return {
          y: (data[yAxis.dataKey] ?? 0) as number,
          x: 10 * index
        };
      });
      const trendLineDataConfig = tableChartTrendDataCalcHelper(trendLineRawData);
      if (transformData?.length) {
        const trendDataKey = `${!!yAxis?.label?.length ? yAxis?.label : yAxis?.key}${TREND_LINE_DIFFERENTIATER}`;
        forEach(trendLineRawData, (args: { x: number }, index) => {
          transformData[index] = {
            ...(transformData[index] ?? {}),
            [trendDataKey]: Math.max(0, trendLineDataConfig.calcY(args.x))
          };
        });
        trendLineData[yAxis.dataKey] = trendDataKey;
      }
    }
  });

  if (showBaseLines) {
    baseLineMap = getBaselineMappings(data, rows, columns);
  }
  const baseLineValues = getBaselineValues(showBaseLines, baseLineMap);

  return {
    data: transformData,
    trendLineData,
    baseLinesDataPoints: baseLineValues,
    baseLineMap,
    chart_props: {
      ...chartProps,
      unit: yUnit,
      display_colors: yDisplayColors,
      areaProps,
      dataMax,
      dataMin
    }
  };
};

export const configureWidgetSingleStatTransform = (data: any) => {
  const yAxis = get(data, ["yAxis"], []);
  const columns = get(data, ["columns"], []);
  const yUnit = get(data, ["yUnit"], "");
  const rows = get(data, ["rows"], []);
  const filters = get(data, ["filters"], []);
  const fr = getFilteredRows(filters, rows, columns);
  const mappedRowsColumns = mapColumnsWithRowsById(fr, columns);
  const yColumn = mappedRowsColumns.find(yCol => yCol.id === yAxis[0].key);
  let stat: number = 0;
  if (yColumn) {
    // getXKeys is doing something not quire right
    const xKeys = getXKeys([], yColumn, mappedRowsColumns);
    const filteredYValues = xKeys
      .map((value: string) => yColumn.values?.[value])
      .filter((value: string) => !isNaN(parseInt(value)))
      .map((value: string) => parseFloat(value));
    if (filteredYValues.length) {
      const yColumnFilter = parseFloat(get(filters, [yAxis[0].key], ""));
      if (!isNaN(yColumnFilter)) {
        if (filteredYValues.includes(yColumnFilter)) {
          stat = yColumnFilter;
        }
      } else {
        stat = filteredYValues.reduce((acc: number, value: number) => acc + value, 0);
      }
    }
  }

  return {
    stat,
    unit: yUnit,
    showRoundedValue: false,
    precision: 2
  };
};

export const transformStackMap = (stackMap: { [key: string]: any }, maxStackedEntries: number) => {
  let truncatedStackArray: any[] = [];
  let newStackMap: { [key: string]: any } = {};
  let newStackMapArray: { [key: string]: any[] } = {};
  let keysInData: { [key: string]: boolean } = {};

  Object.keys(stackMap).forEach((key: any) => {
    const item = stackMap[key];
    let stackArray: { [key: string]: any } = Object.keys(item).map(key => {
      return { key, value: item[key] };
    });

    stackArray.sort((item_a: any, item_b: any) => {
      const a = item_a.value || 0;
      const b = item_b.value || 0;

      if (a > b) {
        return -1;
      } else if (a < b) {
        return 1;
      } else {
        if (item_a.key > item_b.key) {
          return 1;
        } else if (item_a.key < item_b.key) {
          return -1;
        }
        return 0;
      }
    });

    truncatedStackArray = stackArray.slice(0, maxStackedEntries);
    const overflowArray = stackArray.slice(maxStackedEntries);
    const otherSum = overflowArray.reduce((sum: any, currentValue: any) => {
      return sum + currentValue.value;
    }, 0);

    if (otherSum) {
      let insertIndex = truncatedStackArray.findIndex(item => otherSum >= item.value);
      const otherSumData = { key: "Other", value: otherSum };
      if (insertIndex === -1) {
        truncatedStackArray.push(otherSumData);
      } else if (insertIndex === 0) {
        truncatedStackArray.unshift(otherSumData);
      } else {
        truncatedStackArray.splice(insertIndex, 0, otherSumData);
      }
    }

    truncatedStackArray.forEach(item => {
      if (!newStackMap[key]) {
        newStackMap[key] = {};
      }

      keysInData[item.key] = true;
      newStackMap[key][item.key] = item.value;
    });

    newStackMapArray[key] = truncatedStackArray;
  });

  return { stackMap: newStackMap, stackMapArray: newStackMapArray, keysInData };
};

interface CreateStackDataArgs {
  fr?: any;
  xAxisCol?: any;
  isStacked?: boolean;
  stackCols?: any[];
  maxStackedEntries?: number;
  useOrderedStacks?: boolean;
  maxRecords?: number;
}

export const getBarName = (key: string) => (val: any) => val[key]?.key || "";
export const getBarDataKey = (key: string) => (val: any) => val[key]?.value || 0;

export const createStackData = (args: CreateStackDataArgs = {}) => {
  const { fr, xAxisCol, isStacked, stackCols, maxStackedEntries, useOrderedStacks, maxRecords } = args;
  let transformData: any[] = [];
  let fullStackMap: { [key: string]: any } = {};
  // Note: stackMap must remain empty if !isStacked
  let stackMap: { [key: string]: any } = {};
  let barPropsMap: { [key: string]: any } = {
    Other: { name: "Other", dataKey: "Other" }
  };
  let orderedBarPropsMap: { [key: string]: any } = {};

  // Get rid of TS warnings.
  if (!fr || !xAxisCol || !stackCols || typeof maxStackedEntries !== "number" || typeof maxRecords !== "number") {
    return { transformData, barPropsMap, orderedBarPropsMap };
  }

  const fullCountsCols = countBy(fr, function (rec: any) {
    if (xAxisCol) {
      const xIndex = rec[xAxisCol.dataIndex];
      return xIndex;
    }
    return false;
  });

  // Populate stackMap and barPropsMap
  // O(N), assuming that stackCols will usually
  // be limited.
  if (isStacked && xAxisCol) {
    fr.forEach((record: any) => {
      const xIndex = record[xAxisCol.dataIndex];

      if (!fullStackMap[xIndex]) {
        fullStackMap[xIndex] = {};
      }
      stackCols.forEach((stackCol: any) => {
        let stackIndex = record[stackCol.dataIndex];
        let stackName = stackIndex ? stackIndex : UNDEFINED;
        if (!fullStackMap[xIndex][stackIndex]) {
          fullStackMap[xIndex][stackIndex] = 0;
        }
        fullStackMap[xIndex][stackIndex] = 1 + fullStackMap[xIndex][stackIndex];

        if (!barPropsMap[stackIndex]) {
          barPropsMap[stackIndex] = {
            name: stackName,
            dataKey: stackIndex
          };
        }
      });
    });
  }

  // Truncate according to user-specified max x-axis entries.
  const countsCols: { [key: string]: any } = {};
  const fullCountsColsKeys = Object.keys(fullCountsCols);
  const loopLimit = Math.min(fullCountsColsKeys.length, maxRecords);
  for (let i = 0; i < loopLimit; i++) {
    const key = fullCountsColsKeys[i];

    // key should always be defined, but just in case...
    if (key) {
      countsCols[key] = fullCountsCols[key];
      if (isStacked) {
        stackMap[key] = fullStackMap[key];
      }
    }
  }

  // Sorts the stacks so that the biggest are at the bottom.
  // Also limits number of stacks according to user-supplied maxStackedEntries
  // and groups everything else into a stack called "Other"
  const transformResult = transformStackMap(stackMap, maxStackedEntries);
  stackMap = transformResult.stackMap;
  const stackMapArray = transformResult.stackMapArray;
  const keysInData = transformResult.keysInData;

  // Big optimization: Trim down barPropsMap to
  // only contain keys that exist in the data.
  if (isStacked && useOrderedStacks) {
    barPropsMap = pick(barPropsMap, Object.keys(keysInData));
  }

  Object.keys(barPropsMap).forEach((barPropsMapKey, index) => {
    const key = `${index}`;
    const fill = lineChartColors[index % lineChartColors.length];
    barPropsMap[barPropsMapKey].fill = fill;
    orderedBarPropsMap[key] = {
      name: barPropsMap[barPropsMapKey].name,
      dataKey: barPropsMapKey,
      fill
    };
  });

  transformData = Object.keys(countsCols).map(key => {
    let datum: { [key: string]: any } = {
      name: key
    };
    if (isStacked) {
      if (useOrderedStacks) {
        const stackData: { [key: string]: any } = {};

        stackMapArray[key].forEach((item, index) => {
          const innerKey = `${index}`;
          stackData[innerKey] = item;
        });

        datum = {
          ...datum,
          ...stackData
        };
      } else {
        datum = {
          ...datum,
          ...stackMap[key]
        };
      }
    } else {
      datum = {
        ...datum,
        count: countsCols[key]
      };
    }

    return datum;
  });

  return {
    transformData,
    barPropsMap: barPropsMap,
    orderedBarPropsMap: orderedBarPropsMap
  };
};

export const configureWidgetGroupByTransform = (data: any, chartProps: any) => {
  const showBaseLines = get(data, ["showBaseLines"]);
  const showTrendLines = get(data, ["showTrendLines"]);
  const xAxis = get(data, ["xAxis"], "");
  const xAxisSort = get(data, ["xAxisSort"]);
  const columns = get(data, ["columns"], []);
  const rows = get(data, ["rows"], []);
  const filters = get(data, ["filters"], {});
  const stackBy = get(data, ["stackBy"], []);
  const mappedRowsColumns = mapColumnsWithRowsById(rows, columns);
  const xColumn = mappedRowsColumns.find(column => column.id === xAxis);
  const maxStackedEntries = get(data, ["maxStackedEntries"], DEFAULT_MAX_STACKED_ENTRIES);
  const maxRecords = get(data, ["maxRecords"], DEFAULT_MAX_RECORDS);
  const isStacked = !!stackBy.length;
  const fr = getFilteredRows(filters, rows, columns);
  let baseLineMap: any = {};
  let trendLineKey: any = "";

  const stackCols = stackBy
    .map((colId: string) => {
      return columns.find((col: any) => col.id === colId);
    })
    .filter((col: any) => !!col);

  const xAxisCol = columns.find((col: { id: any; dataIndex: any }) => col.id === xAxis);
  const useOrderedStacks = isStacked;

  const args = { fr, xAxisCol, isStacked, stackCols, maxStackedEntries, useOrderedStacks, maxRecords };

  let { transformData, barPropsMap, orderedBarPropsMap } = createStackData(args);
  let barProps = [
    {
      name: "count",
      dataKey: "count",
      unit: "count"
    }
  ];
  let dataKey = chartProps?.barProps?.[0]?.dataKey ?? barProps?.[0]?.dataKey;
  let orderedBarProps = [];

  if (isStacked) {
    barProps = Object.keys(barPropsMap).map(key => {
      return barPropsMap[key];
    });

    orderedBarProps = Object.keys(orderedBarPropsMap).map(key => {
      return orderedBarPropsMap[key];
    });
  }
  // handle sorting if xaxis sort is selected
  if (xAxisSort) {
    if (stackBy && stackBy?.length > 0) {
      transformData = stackedSorting(transformData, xAxisSort, "value", xColumn?.type) ?? [];
    } else {
      transformData = xAxisSorting(transformData, xAxisSort, dataKey, xColumn?.type) ?? [];
    }
  }
  // value on bar
  if (stackBy && stackBy?.length > 0) {
    transformData = transformData.map(data => {
      data.totalStacksBarCount = map(data, "value")?.reduce((a, b) => (b ? a + b : a), 0);
      return data;
    });
  }

  if (showTrendLines) {
    const trendLineRawData = transformData.map((data: Record<string, string | number>, index: number) => {
      return {
        y: (stackBy && stackBy?.length > 0 ? data?.totalStacksBarCount : dataKey ? data?.[dataKey] : 0) as number,
        x: 10 * index
      };
    });
    const trendLineDataConfig = tableChartTrendDataCalcHelper(trendLineRawData);
    if (transformData?.length) {
      const trendDataKey = `${"totalStacksBarCount"}${TREND_LINE_DIFFERENTIATER}`;
      forEach(trendLineRawData, (args: { x: number }, index) => {
        transformData[index] = {
          ...(transformData[index] ?? {}),
          [trendDataKey]: Math.round(Math.max(0, trendLineDataConfig.calcY(args.x)))
        };
      });
      trendLineKey = trendDataKey;
    }
  }
  if (showBaseLines) {
    baseLineMap = getBaselineMappings(data, rows, columns);
  }
  const baseLineValues = getBaselineValues(showBaseLines, baseLineMap);
  return {
    data: transformData,
    baseLinesDataPoints: baseLineValues,
    baseLineMap,
    trendLineKey,
    chart_props: {
      ...chartProps,
      barProps,
      barPropsMap,
      orderedBarProps,
      useOrderedStacks,
      stacked: isStacked,
      unit: "Count",
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 5, left: 5, bottom: 50 }
      }
    }
  };
};

export const getFilteredRows = (filters: any, rows: any, columns: any) => {
  let filteredRows = [...rows];
  Object.keys(filters).forEach(filter => {
    const col = columns.find((col: any) => col.id === filter);
    if (col) {
      filteredRows = filteredRows.filter(row => {
        if (!!filters[filter] && col.dataIndex in row) {
          if (isArray(filters[filter])) {
            return filters[filter]?.includes(row[col.dataIndex]);
          } else if (isObject(filters[filter])) {
            return row[col.dataIndex] in filters[filter];
          }
        }
        return false;
      });
    }
  });
  return filteredRows;
};

export const getXKeys = (filters: any, selectedColumn: any, mappedRowsColumns: any) => {
  let suffixes: any[] = [];
  let xKeys: any[] = [];

  const filterKeys = Object.keys(filters);
  if (filterKeys.length) {
    filterKeys.forEach(key => {
      const columnSuffixes: any[] = [];
      const fColumn = mappedRowsColumns.find((col: { id: string }) => col?.id === key);
      if (fColumn) {
        const val = Object.keys(fColumn.values || {}).filter(fVal => filters[key].includes(fColumn.values?.[fVal]));
        if (val) {
          val.forEach((v: any) => columnSuffixes.push(v.split("row_")[1].split("_")[0]));
          suffixes.push(columnSuffixes);
        }
      }
    });

    suffixes = intersection(...suffixes);

    if (suffixes.length > 0) {
      const rows = suffixes.map(s => `row_${s}_${selectedColumn.id}`);
      xKeys = Object.keys(selectedColumn.values || {}).filter(key => rows.includes(key));
    }
  } else {
    xKeys = Object.keys(selectedColumn.values || {});
  }

  return xKeys;
};

/***
 * @return sorted array as per sortOrder varaible for tables
 * @param  {Array} arr Array of object to be sorted
 * @param {String} dataKey Sorting key to help sort
 * @param {String} sortOrder String key that help sort direction
 * */
export const xAxisSorting = (arr: Array<Record<string, any>>, sortOrder: string, dataKey: string, dataType: string) => {
  switch (sortOrder) {
    case widgetDataSortingOptionKeys.VALUE_LOW_HIGH:
      return arr.sort((a, b) => ((a?.[dataKey] ?? 0) > (b?.[dataKey] ?? 0) ? 1 : -1));
    case widgetDataSortingOptionKeys.VALUE_HIGH_LOW:
      return arr.sort((a, b) => ((a?.[dataKey] ?? 0) < (b?.[dataKey] ?? 0) ? 1 : -1));
    case widgetDataSortingOptionKeys.LABEL_LOW_HIGH: {
      return arr.sort(genericSortingComparator("name", "asc", dataType));
    }
    case widgetDataSortingOptionKeys.LABEL_HIGH_LOW: {
      return arr.sort(genericSortingComparator("name", "desc", dataType));
    }
  }
};
/***
 * @return sorted array as per sortOrder varaible for tables Sort stacked data as keys are having nested data structure
 * @param {Array} arr Array of object to be sorted
 * @param {String} dataKey Sorting key to help sort
 * @param {string} sortOrder String key that help sort direction
 * */
export const stackedSorting = (
  arr: Array<Record<string, any>>,
  sortOrder: string,
  dataKey: string,
  dataType: string
) => {
  switch (sortOrder) {
    case widgetDataSortingOptionKeys.VALUE_LOW_HIGH:
      return arr.sort((a, b) => (objectSumByKey(a, dataKey) > objectSumByKey(b, dataKey) ? 1 : -1));
    case widgetDataSortingOptionKeys.VALUE_HIGH_LOW:
      return arr.sort((a, b) => (objectSumByKey(a, dataKey) < objectSumByKey(b, dataKey) ? 1 : -1));
    case widgetDataSortingOptionKeys.LABEL_LOW_HIGH:
      return xAxisSorting(arr, widgetDataSortingOptionKeys.LABEL_LOW_HIGH, dataKey, dataType);
    case widgetDataSortingOptionKeys.LABEL_HIGH_LOW:
      return xAxisSorting(arr, widgetDataSortingOptionKeys.LABEL_HIGH_LOW, dataKey, dataType);
  }
};
