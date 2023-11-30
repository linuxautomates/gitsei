import { getValueFromTimeRange } from "dashboard/graph-filters/components/helper";
import { LevelopsTableReportColumnType } from "dashboard/pages/dashboard-drill-down-preview/drilldown-types/levelopsTableReportTypes";
import { cloneDeep, forEach, get, isArray } from "lodash";
import moment from "moment";
import { updateTimeFiltersValue } from "../widget-api-wrapper/helper";

/**
 * It takes in the filters, columns, widgetMetaData and dashboardMetaData and returns the updated
 * filters
 * @param {any} filters - The filters object that you want to update.
 * @param columns - Array of columns of the table
 * @param {any} widgetMetaData - The widget metadata object.
 * @param {any} dashboardMetaData - The metadata of the dashboard.
 */
export function updateTableFilters(
  filters: any,
  columns: Array<LevelopsTableReportColumnType>,
  widgetMetaData: any,
  dashboardMetaData: any
) {
  const rangeFilterChoice = get(widgetMetaData, ["range_filter_choice"], {});
  const defaultTimeRange = {
    type: "absolute",
    relative: {
      next: {
        unit: "days"
      },
      last: {
        unit: "days"
      }
    }
  };
  const getDaysCount = (value: { $gt: string; $lt: string }) => {
    const diff = parseInt(value.$lt) - parseInt(value.$gt);
    return Math.round(diff / 86400);
  };
  const timeFilterKeys = Object.keys(filters).filter((key: string) => {
    const col = columns.find((col: any) => col.id === key);
    if (col) {
      return col.inputType === "date";
    }
    return false;
  });

  const timeFilters = timeFilterKeys.reduce((acc: any, next: string) => {
    const filterData = filters[next];

    if (isArray(filterData)) {
      return { ...acc, [next]: moment(filterData?.[0]).utc().unix() };
    }

    let _rangeChoice: any = get(rangeFilterChoice, [next], defaultTimeRange);

    if (!Object.keys(rangeFilterChoice).length) {
      // no key present in metadata, treat filter as slicing
      _rangeChoice = "slicing";
    }

    // checking for existing filter
    if (typeof _rangeChoice === "string" && filterData) {
      _rangeChoice = {
        type: _rangeChoice === "absolute" ? "absolute" : "relative",
        absolute: filterData,
        relative: {
          next: { num: 0, unit: "days" },
          last: { num: getDaysCount(filterData), unit: "days" }
        }
      };
    }

    return {
      ...acc,
      [next]: _rangeChoice.type === "relative" ? getValueFromTimeRange(_rangeChoice.relative, true) : filterData
    };
  }, {});

  const updatedTimeFilters = updateTimeFiltersValue(dashboardMetaData, widgetMetaData, timeFilters);
  let newFilters = cloneDeep(filters);
  forEach(timeFilterKeys, key => {
    newFilters[key] = updatedTimeFilters[key];
  });
  return newFilters;
}
