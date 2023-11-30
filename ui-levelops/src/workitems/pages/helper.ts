import { uniq } from "lodash";

export const mergeFilters = (prevFilters: any, newFilters: any) => {
  let mergedFilters: any = {};
  uniq([...Object.keys(prevFilters), ...Object.keys(newFilters)]).forEach(key => {
    if (
      (typeof prevFilters[key] === "object" && !Array.isArray(prevFilters[key])) ||
      (typeof newFilters[key] === "object" && !Array.isArray(newFilters[key]))
    ) {
      mergedFilters = {
        ...mergedFilters,
        [key]: { ...(prevFilters[key] || {}), ...(newFilters[key] || {}) }
      };
    }
    if (Array.isArray(prevFilters[key]) || Array.isArray(newFilters[key])) {
      mergedFilters = {
        ...mergedFilters,
        [key]: [...(prevFilters[key] || []), ...(newFilters[key] || [])]
      };
    }
    if (typeof prevFilters[key] === "string" || typeof newFilters[key] === "string") {
      mergedFilters = {
        ...mergedFilters,
        [key]: newFilters[key] ? newFilters[key] : prevFilters[key] ? prevFilters[key] : ""
      };
    }
  });
  return mergedFilters;
};
