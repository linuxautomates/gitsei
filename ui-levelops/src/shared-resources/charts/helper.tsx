import { cloneDeep, forEach, get, map, max, uniq } from "lodash";
import React from "react";
import { mappedSortValues, treeMapColors } from "./chart-themes";
import { chartTooltipListitemType } from "./chart-types";
import { strReplaceAll, toTitleCase } from "utils/stringUtils";
import { ReferenceArea } from "recharts";
import { COLOR_MAPPING } from "./constant";
import { Deployments } from "../containers/dora-api-container/types";
import { TIME_INTERVAL_TYPES } from "constants/time.constants";

export const SHOW_DOT = "showDot";
export const NO_DATA = "No Data";
export const TABLE_HAS_XUNIT = ["zendesk_top_customers_report", "levelops_assessment_response_time__table_report"];

export const yAxisIdByKey: any = {
  total_tickets: "Tickets",
  number_of_tickets_closed: "Tickets",
  median_resolution_time: "Days",
  average_resolution_time: "Days",
  files_changed_count: "Number of Files",
  "90th_percentile_resolution_time": "Days",
  "Number of tickets": "Total Tickets",
  "Average age": "Days",
  "90th percentile age": "Days",
  "Median Age": "Days"
};

export const compositeYAxisIdByKey: any = {
  total_tickets: "Tickets",
  total_story_points: "Story Points",
  number_of_tickets_closed: "Tickets",
  median_resolution_time: "Days",
  average_resolution_time: "Days",
  "90th_percentile_resolution_time": "Days",
  mean: "Days",
  p90: "Days",
  median: "Days"
};

export const SCM_PRS_REPORTS = ["github_prs_report", "github_commits_report", "github_issues_report"];

export const getInitialFilters = (
  data: any,
  ignoreKeys: string[],
  defaultFilterKey?: string,
  isAzureBacklog = false
) => {
  const filterKeys = (data as any).reduce((acc: string[], item: any) => {
    if (item) {
      if (Object.keys(item)?.[0] === "0" && !isAzureBacklog) {
        return [
          ...acc,
          ...Object.values(item).reduce((ac: string[], value: any) => [...ac, get(value, ["key"], "")], [])
        ];
      }
      return [...acc, ...Object.keys(item)];
    }
  }, []);

  return uniq(filterKeys)
    ?.filter((key: any) => {
      if (key.includes("additional_key")) return false;

      if (!key && key !== "") return false;

      return !ignoreKeys.includes(key) && !key.includes(SHOW_DOT) && !key.includes("original_");
    })
    .reduce((acc: any, next: any) => {
      return { ...acc, [next]: defaultFilterKey ? next.includes(defaultFilterKey) : true };
    }, {});
};

export const getFilteredData = (data: any, filters: Object) => {
  const filterKeys = Object.keys(filters);
  return data.map((item: any) => {
    if (item) {
      if (Object.keys(item)[0] === "0") {
        let final_filters = { ...item };
        Object.keys(item).map((value: any, index: number) => {
          if (value && get(item[value], ["key"], "")) {
            if (filterKeys.includes(item[value]["key"]) && !(filters as any)[item[value]["key"]]) {
              delete final_filters[value];
            }
            final_filters = { ...final_filters };
          }
        });
        return final_filters;
      }
      return Object.keys(item).reduce((acc, next) => {
        if (filterKeys.includes(next)) {
          if ((filters as any)[next]) {
            return { ...acc, [next]: (item as any)[next] };
          }
          return acc;
        }
        return { ...acc, [next]: (item as any)[next] };
      }, {});
    }
  });
};

export const onFilterChange = (key: string, value: boolean, filters: Object, setFilters: Function, id: string) => {
  const widgetFilters = (filters as any)[id];

  // condition to keep at least one checkbox checked
  if (widgetFilters && Object.keys(widgetFilters).filter(key => (widgetFilters as any)[key]).length === 1 && !value) {
    return;
  }

  if (widgetFilters && (widgetFilters as any)[key] !== value) {
    const updatedFilters = { ...widgetFilters, [key]: value };
    return setFilters(id, updatedFilters);
  }

  setFilters(id, { [key]: value });
};

export const getTreeMapItemColor = (value: number): string => {
  if (value) {
    if (value > 0 && value <= 25) {
      return treeMapColors[0];
    } else if (value > 25 && value <= 50) {
      return treeMapColors[1];
    } else return treeMapColors[2];
  }
  return "";
};

export const getMappedSortValue = (sort_value: string) => {
  return mappedSortValues[sort_value] || sort_value;
};

export const svgToPng = (svg: any, width: number = 900, height: number = 400) => {
  return new Promise((resolve, reject) => {
    let canvas = document.createElement("canvas");
    let svgSize = svg.getBoundingClientRect();
    const scale = parseInt(svgSize.width) < 1000 ? 1 : 1;
    //canvas.width = width;
    //canvas.height = height;
    canvas.width = svgSize.width;
    canvas.height = svgSize.height;
    canvas.style.width = svgSize.width;
    canvas.style.height = svgSize.height;

    let ctx = canvas.getContext("2d");
    // @ts-ignore
    ctx.scale(scale, 1);
    // Set background to white
    // @ts-ignore
    ctx.fillStyle = "#ffffff";
    // @ts-ignore
    ctx.fillRect(0, 0, svgSize.width * scale, svgSize.height);
    //ctx.fillRect(0, 0, svgSize.width, svgSize.height);

    let xml = new XMLSerializer().serializeToString(svg);
    let dataUrl = "data:image/svg+xml;utf8," + encodeURIComponent(xml);
    //let img = new Image(svgSize.width, svgSize.height);
    let img = new Image(svgSize.width * scale, svgSize.height);

    img.onload = () => {
      // @ts-ignore
      ctx.drawImage(img, 0, 0);
      let imageData = canvas.toDataURL("image/png", 1.0);
      resolve(imageData);
    };

    img.onerror = () => reject();

    img.src = dataUrl;
  });
};

export const convertChart = async (ref: any) => {
  if (ref && ref.container) {
    let svgRef = ref.container.children[0];
    if (svgRef) {
      return svgToPng(svgRef);
    }
  }
  return null;
};

export const simplifyValueInDays = (value: number) => {
  let simplifiedValue = value;
  if (simplifiedValue >= 1 || simplifiedValue === 0) {
    return {
      simplifiedValue,
      unit: "Per Day"
    };
  }
  simplifiedValue = value * 7;
  if (simplifiedValue >= 1) {
    return {
      simplifiedValue,
      unit: "Per Week"
    };
  }
  simplifiedValue = value * 30;
  return {
    simplifiedValue,
    unit: "Per Month"
  };
};

export const getNumberAbbreviation = (
  value: number,
  shouldRoundOutputValue: boolean = true,
  precision: number | undefined = undefined,
  simplifyValue: boolean = false
) => {
  if (simplifyValue) {
    const { simplifiedValue } = simplifyValueInDays(value);
    return Math.round(simplifiedValue * 100) / 100 + "";
  }
  let [n2, n3] = [Math.abs(value), 0];
  while (n2 >= 1000) {
    n2 /= 1000;
    n3++;
  }

  if (shouldRoundOutputValue) {
    n2 = Math.round(n2);
  }

  if (!shouldRoundOutputValue && !!precision && !Number.isInteger(n2)) {
    const fixedNumber = parseFloat(n2.toFixed(precision));
    return fixedNumber + ["", "K", "M", "B", "T"][n3];
  }

  return n2 + ["", "K", "M", "B", "T"][n3];
};

export const getSimplifiedUnit = (value: number, unitSymbol: string | undefined, simplifyValue: boolean = false) => {
  if (simplifyValue) {
    const { unit } = simplifyValueInDays(value);
    return unit;
  }
  return unitSymbol;
};

export const getShowDotData = (data: any, keys: string[], index: number) => {
  return keys.reduce((acc: any, key: string): any => {
    const dotKey = `${SHOW_DOT}_${key}`;
    const prevData = data[index - 1];
    const nextData = data[index + 1];
    const currentData = data[index];

    let hasDot = false;

    if (index === 0 && currentData && nextData) {
      hasDot = currentData[key] !== NO_DATA && nextData[key] === NO_DATA;
    }

    if (index === data.length - 1 && currentData && prevData) {
      hasDot = currentData[key] !== NO_DATA && prevData[key] === NO_DATA;
    }

    if (index > 0 && index < data.length - 1 && currentData && nextData && prevData) {
      hasDot = currentData[key] !== NO_DATA && prevData[key] === NO_DATA && nextData[key] === NO_DATA;
    }
    return {
      ...acc,
      [dotKey]: hasDot
    };
  }, {});
};

export const getTableClick = (reportType: string, xUnit: string = "") => {
  return TABLE_HAS_XUNIT.includes(reportType) ? xUnit : " ";
};

export const formatTooltipValue = (value: any, range: number = 1) => {
  // Checking if value is a float, truncate the decimals when its true
  if (value === +value && value !== (value | 0)) {
    value = value.toFixed(range);
  }
  return value;
};

export const transformDataKey = (key: string) => {
  let keyName = key;
  if (keyName.includes("bar")) {
    keyName = keyName.substr(0, keyName.length - 3);
  } else if (keyName.includes("area") || keyName.includes("line")) {
    keyName = keyName.substr(0, keyName.length - 4);
  }
  keyName = strReplaceAll(keyName || "", "^__", " ");
  // keyName = strReplaceAll(keyName || "", "^", " ");
  return keyName || "";
};

export const transformKey = (key: string) => {
  let keyName = key;
  if (keyName.includes("bar")) {
    keyName = keyName.substr(0, keyName.length - 3);
  } else if (keyName.includes("area") || keyName.includes("line")) {
    keyName = keyName.substr(0, keyName.length - 4);
  }
  keyName = strReplaceAll(keyName || "", "_", " ");
  keyName = strReplaceAll(keyName || "", "^", " ");
  return keyName || "";
};

export const getFilterWidth = (title: string, font = "14px Inter") => {
  const element = document.createElement("canvas");
  let context = element.getContext("2d");
  if (context) {
    context.font = font;
  }
  return context ? context.measureText(title).width : 0;
};

// Funtion for sorting tooltip items

export const sortTooltipListItems = (listItems: chartTooltipListitemType[], allowLabelTransform = true) => {
  let curList = cloneDeep(listItems);
  curList.sort((value1, value2) => {
    const key1 = (value1?.label || "").toLowerCase().trim();
    const key2 = (value2?.label || "").toLowerCase().trim();
    if (key1 < key2) return -1;
    if (key1 > key2) return 1;
    return 0;
  });

  const totalCountItem = (curList || []).find(item => item.label === "Total");
  if (totalCountItem) {
    curList = (curList || []).filter(item => item.label !== "Total");
    curList.push(totalCountItem);
  }

  return curList.map(item => {
    if (!item?.value && !item?.label && typeof item?.label === "function") {
      return null;
    }

    const label = allowLabelTransform ? toTitleCase(item?.label || "") : item?.label;

    if (item?.label === "No Data") {
      return (
        <li key={item?.label} style={{ margin: "0.5rem 0.5rem 0 0.5rem" }}>
          {label}
        </li>
      );
    }

    return (
      <li key={item?.label}>
        <span style={{ color: item?.color, fontSize: "10px" }}>{`● `}</span>
        {`${label}: ${item?.value}`}
      </li>
    );
  });
};

export const CHECKBOX_PADDING = 16;
export const CHECKBOX_WIDTH = 16;
export const CHECKBOX_SPACE = 8;
export const MORE_BUTTON_WIDTH = 52;
export const RESET_BUTTON_WIDTH = 92;
export const STACK_LEGEND_MAX_FILTERS = 3;

export const TOMANYDATAPOINTMESSGAE = "Too many data points to display";
export const INSUFFICIENTDATAPOINTMESSAGE = "Insufficient data points to display";

/**
 * Get the maximum value for the Y axis domain, rounding up to the nearest multiple of the interval.
 * @param {number} dataMax - The maximum value of the data set.
 * @param {number} [interval=5] - The number of ticks you want on the y-axis.
 * @returns the max value of the data set rounded up to the nearest multiple of the interval.
 */
export function getYAxisDomainMax(dataMax: number, interval: number = 5) {
  const parts = interval - 1;
  dataMax = Math.ceil(dataMax ?? 0);
  if (dataMax % parts === 0) return dataMax;
  return dataMax + (parts - (dataMax % parts));
}

export const getDomainMax = (data: any, stacked: any) => {
  let maximumValue = Number.MIN_SAFE_INTEGER;
  if (stacked) {
    const arr = data.map((temp: any) => map(temp, "value")?.reduce((a: any, b: any) => (b ? a + b : a), 0));
    return Math.max(...arr);
  }
  forEach(data, d => {
    if (Object.keys(d).length) {
      const values: any = Object.values(d).filter(v => typeof v === "number");
      maximumValue = Math.max(maximumValue, max(values) ?? maximumValue);
    }
  });
  if (maximumValue < 0) return 0;
  return maximumValue;
};

export const rechartReferenceArea = (y1: number | undefined, y2: number | undefined, color: string) => {
  return <ReferenceArea y1={y1} y2={y2} fill={color} fillOpacity={0.3} />;
};

export const COLOR_LOW_TO_HIGH = [COLOR_MAPPING.low, COLOR_MAPPING.lower_mid, COLOR_MAPPING.mid, COLOR_MAPPING.high];

export const getReferenceAreas = (baseLinesDataPoints: any, baseLineMap: any, data: any, stacked: boolean) => {
  const baseLinesCount = baseLinesDataPoints?.length;
  if (baseLinesCount) {
    if (baseLinesCount === 2) {
      return baseLineMap[baseLinesDataPoints?.[0] || ""] === COLOR_MAPPING.low ||
        baseLineMap[baseLinesDataPoints?.[0] || ""] === undefined
        ? [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.low
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              undefined,
              COLOR_MAPPING.lower_mid
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[1] : undefined,
              undefined,
              COLOR_MAPPING.high
            )
          ]
        : [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.high
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              undefined,
              COLOR_MAPPING.lower_mid
            ),
            rechartReferenceArea(baseLinesDataPoints ? baseLinesDataPoints[1] : undefined, undefined, COLOR_MAPPING.low)
          ];
    } else if (baseLinesCount === 1) {
      return baseLineMap[baseLinesDataPoints?.[0] || ""] === COLOR_MAPPING.low
        ? [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.high
            ),
            rechartReferenceArea(baseLinesDataPoints ? baseLinesDataPoints[0] : undefined, undefined, COLOR_MAPPING.low)
          ]
        : [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.low
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              undefined,
              COLOR_MAPPING.high
            )
          ];
    } else if (baseLinesCount >= 3) {
      return baseLineMap[baseLinesDataPoints?.[0] || ""] === COLOR_MAPPING.low
        ? [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.low
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              baseLinesDataPoints ? baseLinesDataPoints[1] : undefined,
              COLOR_MAPPING.lower_mid
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[1] : undefined,
              undefined,
              COLOR_MAPPING.mid
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[2] : undefined,
              undefined,
              COLOR_MAPPING.high
            )
          ]
        : [
            rechartReferenceArea(
              undefined,
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              COLOR_MAPPING.high
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[0] : undefined,
              baseLinesDataPoints ? baseLinesDataPoints[1] : undefined,
              COLOR_MAPPING.mid
            ),
            rechartReferenceArea(
              baseLinesDataPoints ? baseLinesDataPoints[1] : undefined,
              undefined,
              COLOR_MAPPING.lower_mid
            ),
            rechartReferenceArea(baseLinesDataPoints ? baseLinesDataPoints[2] : undefined, undefined, COLOR_MAPPING.low)
          ];
    }
  }
  return [];
};

export const getBaselineYAxisTicks = (yAxisProps: any, baseLinesDataPoints: any, baseLineMap: any) => {
  const { x, y, index, payload } = yAxisProps;
  let color: string = "";
  color = baseLineMap?.[payload?.value];
  if (!color) {
    color = index === 0 ? COLOR_MAPPING.low : COLOR_MAPPING.high;
  }
  return (
    <g transform={`translate(${x},${y})`}>
      <text dy={4} fill={color} style={{ fontSize: "0.75rem", fillOpacity: 1 }}>
        {get(yAxisProps, ["payload", "value"], "")}
      </text>
    </g>
  );
};

export const simplifyValueByInterval = (
  value: Deployments,
  interval: TIME_INTERVAL_TYPES.DAY | TIME_INTERVAL_TYPES.WEEK | TIME_INTERVAL_TYPES.MONTH,
  dfConfigurableWeekMonth: boolean
) => {
  if (dfConfigurableWeekMonth) {
    switch (interval) {
      case TIME_INTERVAL_TYPES.DAY:
        return {
          simplifiedValue: value?.count_per_day,
          unit: "Per Day"
        };
        break;
      case TIME_INTERVAL_TYPES.WEEK:
        return {
          simplifiedValue: value?.count_per_week,
          unit: "Per Week"
        };
        break;
      case TIME_INTERVAL_TYPES.MONTH:
        return {
          simplifiedValue: value?.count_per_month,
          unit: "Per Month"
        };
        break;

      default:
        return simplifyValueInDays((value as Deployments).count_per_day);
        break;
    }
  } else {
    return simplifyValueInDays((value as Deployments).count_per_day);
  }
};

const paddingAndMargin = CHECKBOX_PADDING + CHECKBOX_WIDTH + CHECKBOX_SPACE;

/**
 * Generic method to calculate pivot index to display `More` option on legend.
 * @param filterKeys
 * @param legendFormatter
 * @param width
 * @returns index of filterKeys
 */
export const calcLegendPivotIndex = (filterKeys: string[], legendFormatter: (value: any) => any, width?: number) => {
  let contendWidth = RESET_BUTTON_WIDTH + MORE_BUTTON_WIDTH + 20;
  let absolutePivotIndex = filterKeys.length;
  for (let index = 0; index < filterKeys.length; index++) {
    const key = filterKeys[index];
    contendWidth = contendWidth + getFilterWidth(legendFormatter(key || "")) + paddingAndMargin;
    if (width && contendWidth >= width) {
      absolutePivotIndex = index;
      break;
    }
  }
  absolutePivotIndex = Math.min(absolutePivotIndex, STACK_LEGEND_MAX_FILTERS);
  return absolutePivotIndex;
};
