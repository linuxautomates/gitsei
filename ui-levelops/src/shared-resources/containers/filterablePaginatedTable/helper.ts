import { SortOptions } from "./constants";
import {
  KEY_BEGINS,
  KEY_CONTAINS,
  KEY_ENDS,
  KEY_EQUALS,
  KEY_GREATER_THAN,
  KEY_LESS_THAN
} from "configurations/pages/TrellisProfile/constant";
import { numberSortingComparator, stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { rawStatsColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { toTitleCase } from "utils/stringUtils";

export const getIconName = (isFilterAvailable: boolean, sortOrder?: SortOptions): string => {
  if (isFilterAvailable) {
    switch (sortOrder) {
      case SortOptions.ASC:
        return "tableFilterSortAsc";
      case SortOptions.DESC:
        return "tableFilterSortDesc";
      default:
        return "tableFilter";
    }
  } else if (sortOrder) {
    if (sortOrder === SortOptions.ASC) {
      return "tableSortAsc";
    } else {
      return "tableSortDesc";
    }
  }
  return "columnFilterEmpty";
};

export const filterData = (dataSource: Array<any>, filters: Map<string, { key: string; value: string }>) =>
  dataSource?.filter((item: any) => {
    if (!item) return true;
    // @ts-ignore
    for (const [index, filter] of filters.entries()) {
      switch (filter.key) {
        case KEY_CONTAINS:
          if (!item[index]?.toLowerCase().includes(filter.value.toLowerCase())) {
            return false;
          }
          break;
        case KEY_BEGINS:
          if (!item[index]?.toLowerCase().startsWith(filter.value.toLowerCase())) {
            return false;
          }
          break;
        case KEY_ENDS:
          if (!item[index]?.toLowerCase().endsWith(filter.value.toLowerCase())) {
            return false;
          }
          break;
        case KEY_EQUALS:
          // @ts-ignore
          if (!(item[index] === filter.value || (filter.value === 0 && !item[index]))) {
            return false;
          }
          break;
        case KEY_GREATER_THAN:
          if (!(item[index] > filter.value)) {
            return false;
          }
          break;
        case KEY_LESS_THAN:
          if (item[index] >= filter.value) {
            return false;
          }
          break;
      }
    }
    return true;
  });

export const sortData = (
  data: Array<any>,
  sortList: { index: string; order: SortOptions; isNumeric: boolean } | null
) => {
  if (sortList) {
    let sortedData = [];
    if (sortList.isNumeric) {
      sortedData = data.sort(numberSortingComparator(sortList.index, sortList.order));
    } else {
      sortedData = data.sort(stringSortingComparator(sortList.index, sortList.order));
    }
    return [...sortedData];
  }
  return data;
};

export const getSortFilters = (
  sortList: { index: string; order: SortOptions; isNumeric: boolean } | null,
  filters: Map<string, { key: string; value: string }>
): { title: string; sortFilterList: string[] } | undefined => {
  let title = "";
  let sortFilterList = new Set<string>();
  let sortFilterType = "";
  if (sortList) {
    sortFilterType = sortFilterType.concat("Sort");
    const column = rawStatsColumns[sortList.index];
    const columnTitle = column
      ? typeof column.title === "string"
        ? column.title
        : column.titleForCSV
      : toTitleCase(sortList.index);
    if (columnTitle) {
      sortFilterList.add(columnTitle);
    }
  }
  if (filters.size) {
    sortFilterType = sortFilterType.concat("Filter");
    filters.forEach((value: { key: string; value: string }, key: string) => {
      const column = rawStatsColumns[key];
      const columnTitle = column
        ? typeof column.title === "string"
          ? column.title
          : column.titleForCSV
        : toTitleCase(key);
      if (columnTitle) {
        sortFilterList.add(columnTitle);
      }
    });
  }
  switch (sortFilterType) {
    case "Sort":
      title = "Sort applied to: ";
      break;
    case "Filter":
      title = "Filters applied to: ";
      break;
    case "SortFilter":
      title = "Sort & Filters applied to: ";
      break;
  }
  if (title && sortFilterList.size > 0) {
    return { title, sortFilterList: Array.from(sortFilterList) };
  }
  return undefined;
};
