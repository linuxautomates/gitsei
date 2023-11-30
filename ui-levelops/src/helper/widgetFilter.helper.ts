import { AZURE_CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_PREFIX, TESTRAILS_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { sanitizeFilterObject } from "./../utils/filtersUtils";

export const getFilterValue = (
  allFilters: any,
  keyToFind: string,
  isFilterKeyObj?: boolean,
  keysToFindIn?: string[],
  partialKey?: string,
  excludeKey?: string,
  getExcludeWithPartialMatchKeyFlag?: boolean
): any => {
  const sanitizedFilters = Object.keys(allFilters).reduce((acc: any, item: string) => {
    if (allFilters[item] !== undefined && allFilters[item] !== null) {
      acc = { ...acc, [item]: allFilters[item] };
      return acc;
    }
    return acc;
  }, {});
  const topLevelKeys = Object.keys(sanitizedFilters);
  const isCustomField = keyToFind.includes(CUSTOM_FIELD_PREFIX) || keyToFind.includes(AZURE_CUSTOM_FIELD_PREFIX) || keyToFind.includes(TESTRAILS_CUSTOM_FIELD_PREFIX);
  const filterKeyObj = isFilterKeyObj ? sanitizedFilters : sanitizedFilters?.filter;
  // find value only in the keys provided
  if (keysToFindIn?.length) {
    let filterValue: any;
    for (const key in keysToFindIn) {
      const filterToFindObj = sanitizeFilterObject(sanitizedFilters?.[keysToFindIn?.[key]]);
      const recursiveCallObj = getFilterValue(filterToFindObj, keyToFind, key === "filter");
      if (Object.keys(recursiveCallObj || {}).length) {
        filterValue = recursiveCallObj;
        break;
      }
    }
    return filterValue;
  }

  // value found at the top level
  if (topLevelKeys.includes(keyToFind)) {
    return { value: sanitizedFilters[keyToFind] };
  }

  // find custom field value with and without exclude
  if (isCustomField) {
    const customFieldsObj = sanitizeFilterObject(filterKeyObj?.["custom_fields"] ?? filterKeyObj?.["workitem_custom_fields"] ?? {});
    const excludeCustomFieldsObj = sanitizeFilterObject(filterKeyObj?.["exclude"]?.["custom_fields"] ?? filterKeyObj?.["exclude"]?.["workitem_custom_fields"] ?? {});
    const customFieldsObjkeys = Object.keys(customFieldsObj);
    const excludeCustomFieldsObjkeys = Object.keys(excludeCustomFieldsObj);
    if (customFieldsObjkeys.includes(keyToFind)) {
      return { value: customFieldsObj[keyToFind] };
    }
    if (excludeCustomFieldsObjkeys.includes(keyToFind)) {
      return { value: excludeCustomFieldsObj[keyToFind], isExcluded: true };
    }
  }

  //find partial filter
  if (filterKeyObj?.partial_match) {
    const partialMatchObj = sanitizeFilterObject(filterKeyObj?.["partial_match"] ?? {});
    const partialMatchObjkeys = Object.keys(partialMatchObj);
    const key = partialKey ?? keyToFind;
    if (partialMatchObjkeys.includes(key)) {
      return { value: partialMatchObj[key], isPartialMatched: true };
    }
  }

  //find in excluded key
  if (filterKeyObj?.exclude) {
    const excludeObj = sanitizeFilterObject(filterKeyObj?.["exclude"] ?? {});
    const excludeObjkeys = Object.keys(excludeObj);
    const key = excludeKey ?? keyToFind;

    if (excludeObjkeys.includes(key)) {
      return { value: excludeObj[key], isExcluded: true };
    }
    if(getExcludeWithPartialMatchKeyFlag && excludeObjkeys.includes('partial_match')){
      const excludeObjkeysPartial = Object.keys(excludeObj?.['partial_match']);
      const keyPartial = partialKey ?? keyToFind;
      if (excludeObjkeysPartial.includes(keyPartial)) {
        return { value: excludeObj['partial_match'], isExcluded: true, isPartialMatched: true };
      }
    }
  }
};
