import { forEach, get } from "lodash";

/**
 * partial filter transform function

 * @param filter the filter object only
 * @param partialFilterKey key used for partial filters
 * @return transformed filter object
 */

export const githubCommittersPartialFilterTransformer = (filters: any, partialFilterKey: string) => {
  const partialFilters = get(filters, [partialFilterKey], {});
  const exceptionKeys = ["committer"];
  if (Object.keys(partialFilters).length > 0) {
    let mappedPartialFilters: any = partialFilters;
    forEach(Object.keys(partialFilters), key => {
      if (exceptionKeys.includes(key)) {
        mappedPartialFilters[key] = partialFilters?.[key]?.[Object.keys(partialFilters[key] || {})[0]];
      }
    });
    return {
      ...(filters || {}),
      [partialFilterKey]: mappedPartialFilters
    };
  }
  return filters;
};
