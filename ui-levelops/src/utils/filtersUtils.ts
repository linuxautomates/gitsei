import { get, isArray, isObject, isString, keys, pickBy, uniq } from "lodash";
import { sanitizeObject } from "./commonUtils";
import { sanitizeRegexString } from "./stringUtils";

const singleValueKeys = ["time_period"];
export const sanitizePartialStringFilters = (filters: any) => {
  let sanitizedFilters: any = {};
  Object.keys(filters).forEach(fil => {
    let _sanitizeObject = sanitizeObject(filters[fil]);
    // sanitizing the partial match in the widget request payload
    _sanitizeObject = Object.keys(_sanitizeObject).reduce(
      (acc, key) => ({ ...acc, [key]: sanitizeRegexString(_sanitizeObject[key]) }),
      {}
    );
    sanitizedFilters[fil] = _sanitizeObject;
  });
  return pickBy(sanitizedFilters, (v: any) => typeof v === "object" && !isArray(v) && Object.keys(v).length > 0);
};

const checkForArrayType = (arr: Array<any>, type: "string" | "number" = "string"): boolean => {
  return arr.every(value => typeof value === type);
};

export const sanitizeFilterObject = (data: any) => {
  return pickBy(data, (value: any) =>
    isArray(value) || isString(value) ? value.length > 0 : isObject(value) && keys(value).length > 0
  );
};

export const sanitizeFilters = (filter: any) => {
  const partialStringFilters = get(filter, ["partial_match"], undefined);
  const excludeFilters = get(filter, ["exclude"], undefined);

  let sanitizedPartialFilters: any = {};
  if (partialStringFilters) {
    sanitizedPartialFilters = sanitizePartialStringFilters(partialStringFilters);
  }

  let sanitizedExcludeFilters: any = {};
  if (excludeFilters) {
    sanitizedExcludeFilters = sanitizeFilterObject(excludeFilters);
  }

  const newFilter = {
    ...(filter || {}),
    partial_match: sanitizedPartialFilters,
    exclude: sanitizedExcludeFilters
  };

  const sanitizedFilter: any = sanitizeFilterObject(newFilter);

  keys(sanitizedFilter).map((key: string) => {
    const value = sanitizedFilter[key];
    // filtering only array values
    if (isArray(value) && checkForArrayType(value)) {
      sanitizedFilter[key] = uniq(value);
    }
  });

  singleValueKeys.forEach(key => {
    if (Object.keys(filter).includes(key)) {
      sanitizedFilter[key] = filter[key];
    }
  });

  return sanitizedFilter;
};