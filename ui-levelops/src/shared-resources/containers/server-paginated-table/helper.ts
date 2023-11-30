import { get, isArray, unset } from "lodash";
import { buildWidgetQuery } from "../../../configurable-dashboard/helpers/queryHelper";

export const buildServerPaginatedQuery = (
  filters: any,
  filterKey: string,
  filterType: string,
  exclude: boolean = false
) => {
  const filterValue = getFilterValue(filters, filterKey, filterType, exclude);
  return buildWidgetQuery(filters, filterValue, filterKey, exclude);
};

export const getFilterValue = (filters: any, filterKey: string, filterType: string, exclude: boolean) => {
  switch (filterType) {
    case "apiSelect":
    case "select":
    case "search":
    case "input": {
      const filterValue = get(filters, [filterKey], "");
      const excludeValue = get(filters, ["exclude", filterKey], undefined);
      return excludeValue ? excludeValue : filterValue;
    }
    case "multiSelect":
    case "apiMultiSelect":
    case "cascade":
    case "dateRange": {
      if (exclude) {
        let filterValue = get(filters, [filterKey], []);

        if (filterKey.includes("customfield_")) {
          filterValue = get(filters, ["custom_fields", filterKey], []);
        }
        return filterValue;
      }
      let excludeFilters = get(filters, ["exclude", filterKey], []);
      if (filterKey.includes("customfield_")) {
        excludeFilters = get(filters, ["exclude", "custom_fields", filterKey], []);
      }
      return excludeFilters;
    }
    default:
      return get(filters, [filterKey], undefined);
  }
};

export const buildMissingFieldsQuery = (prevMissingFields: any, newFilter: string, key: string, value: boolean) => {
  if (!value) {
    unset(prevMissingFields, [newFilter]);
    return {
      ...prevMissingFields
    };
  }
  return {
    ...prevMissingFields,
    [newFilter]: key === "ab"
  };
};

export const setFilterState = (oldValue: any, newValue: any, setValue: (value: any) => void) => {
  if (!newValue) return;

  if (isArray(newValue)) {
    if (newValue?.length === oldValue?.length) {
      oldValue.forEach((ele: any) => {
        if (!newValue.includes(ele)) {
          setValue(newValue);
          return;
        }
      });
    } else {
      setValue(newValue);
    }
  } else {
    if (newValue !== oldValue) {
      setValue(newValue);
    }
  }
};

const hasKeyValue = (filters: any, key: string | undefined, filterKey: string) => {
  if (key && filters && filters[key] && filters[key][filterKey] !== undefined && filters[key][filterKey] !== null) {
    // @ts-ignore
    return typeof filters[key][filterKey] === "object"
      ? Object.keys(filters[key][filterKey]).length > 0
      : filters[key][filterKey].toString().length > 0;
  }

  if (filters[filterKey] !== undefined && filters[filterKey] !== null) {
    return typeof filters[filterKey] === "object"
      ? Object.keys(filters[filterKey]).length > 0
      : filters[filterKey].toString().length > 0;
  }

  return undefined;
};

//Returns, false if filter key has no value and true otherwise
export const hasFilterValue = (morefilter: any, filter: string) => {
  let value;
  if (morefilter?.exclude?.custom_fields) {
    value = hasKeyValue(morefilter?.exclude, "custom_fields", filter);
  }
  if (morefilter?.exclude) {
    value = value || hasKeyValue(morefilter, "exclude", filter);
  }
  if (morefilter?.custom_fields) {
    value = value || hasKeyValue(morefilter, "custom_fields", filter);
  }
  if (morefilter?.partial_match) {
    value = value || hasKeyValue(morefilter, "partial_match", filter);
  }
  return value || hasKeyValue(morefilter, undefined, filter);
};

export const sortingColumnForFixedLeft = (column: any[]) => {
  return column
    .sort((a, b) => {
      if (a.fixed === "left" && b.fixed !== "left") return -1;
      if (a.fixed !== "left" && b.fixed === "left") return 1;
      return 0;
    });
}