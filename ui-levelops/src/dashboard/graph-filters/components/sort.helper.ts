import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { isNaN, lowerCase } from "lodash";

/**
 * This function sorts the data when there is uncertainty of value to be a string or number
 * It takes in an array of objects, and returns a sorted array of objects based on the key and order
 * passed in
 * @param [key=key] - The key to sort by.
 * @param [order=asc] - asc or desc
 * @returns A function that takes two arguments and returns a number.
 */
export const genericSortingComparator = (key = "key", order = "asc", dataType = "") => {
  return (value1: any, value2: any) => {
    // date sort
    if (dataType === "date") {
      return order === "asc"
        ? new Date(value1?.[key])?.valueOf() - new Date(value2?.[key])?.valueOf()
        : new Date(value2?.[key])?.valueOf() - new Date(value1?.[key])?.valueOf();
    }
    let key1: string | number = parseFloat(value1?.[key]),
      key2: string | number = parseFloat(value2?.[key]);
    key1 = isNaN(key1) ? lowerCase(value1?.[key]) : key1;
    key2 = isNaN(key2) ? lowerCase(value2?.[key]) : key2;

    if (order === "asc") {
      if (key1 < key2) return -1;
      if (key1 > key2) return 1;
    } else {
      if (key1 < key2) return 1;
      if (key1 > key2) return -1;
    }
    return 0;
  };
};

export const numberSortingComparator = (key = "key", order = "asc") => {
  // Key is used to sort object values by a specific key.
  // Not removing the extra "label" key logic here coz I am unaware for what places it is used for.
  return (value1: any, value2: any) => {
    if (value1 && value2 && !isNaN(value1[key]) && !isNaN(value2[key])) {
      const key1 = value1[key] ?? 0;
      const key2 = value2[key] ?? 0;
      if (order === "asc") {
        if (key1 < key2) return -1;
        if (key1 > key2) return 1;
      } else {
        if (key1 < key2) return 1;
        if (key1 > key2) return -1;
      }
      return 0;
    }
    return 0;
  };
};

export const stringSortingComparator = (key = "key", order = "asc") => {
  // Key is used to sort object values by a specific key.
  // Not removing the extra "label" key logic here coz I am unaware for what places it is used for.
  return (value1: any, value2: any) => {
    // sorting available options, alphabetically
    if (value1 && !!value1[key] && value2 && !!value2[key]) {
      const key1 = typeof value1[key] === "string" ? lowerCase(value1[key]) : value1[key];
      const key2 = typeof value2[key] === "string" ? lowerCase(value2[key]) : value2[key];
      if (order === "asc") {
        if (key1 < key2) return -1;
        if (key1 > key2) return 1;
      } else {
        if (key1 < key2) return 1;
        if (key1 > key2) return -1;
      }
      return 0;
    }
    // sorting available options, alphabetically
    const key1 = typeof value1?.label === "string" ? lowerCase(value1?.label) : value1?.label;
    const key2 = typeof value2?.label === "string" ? lowerCase(value2?.label) : value2?.label;
    if (order === "asc") {
      if (key1 < key2) return -1;
      if (key1 > key2) return 1;
    } else {
      if (key1 < key2) return 1;
      if (key1 > key2) return -1;
    }
    return 0;
  };
};

export const getSortedFilterOptions = (filterOptions: any) => {
  return (filterOptions ?? []).sort(stringSortingComparator());
};

/** for sorting collection score table */
export const ouScoreTableTransformer = (key: string) => {
  return (score1: basicMappingType<string | number>, score2: basicMappingType<string | number>) => {
    if (
      score1 &&
      score2 &&
      typeof score1[key] === "number" &&
      typeof score2[key] === "number" &&
      score1.name &&
      score2.name
    ) {
      return ((score1 as any)[key] ?? 0) - ((score2 as any)[key] ?? 0);
    }
    return 0;
  };
};
