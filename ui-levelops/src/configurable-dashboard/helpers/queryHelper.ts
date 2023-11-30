import { ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import { appendAndUpdateFilters, getSupportedFilterURI } from "dashboard/components/dashboard-application-filters/helper";
import { TICKET_CATEGORIZATION_SCHEMES_KEY, TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { ADDITIONAL_KEY_FILTERS, AZURE_CUSTOM_FIELD_PREFIX, CONTAINS, CUSTOM_FIELD_PREFIX, STARTS_WITH, TESTRAILS_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import { JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, PARTIAL_FILTER_KEY, PARTIAL_FILTER_MAPPING_KEY } from "dashboard/constants/filter-key.mapping";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get, set, unset } from "lodash";
import { sanitizeObject } from "utils/commonUtils";

export const buildWidgetQuery = (
  prevQuery: any,
  value: any,
  type: any,
  exclude?: boolean,
  customFieldsKey = "custom_fields",
  useMapping = true,
  partialKey?: string,
  otherFlagData: any = {},
) => {
  const { allowExcludeWithPartialMatch } = otherFlagData;
  if (
    type?.includes(CUSTOM_FIELD_PREFIX) ||
    type?.includes(AZURE_CUSTOM_FIELD_PREFIX) ||
    type?.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)
  ) {
    return buildCustomQuery(prevQuery, value, type, exclude, customFieldsKey);
  }
  let filterKey = type;
  if (useMapping) {
    filterKey = get(valuesToFilters, [type], type);
  }
  let newFilters: any;
  if (exclude) {
    newFilters = {
      ...(prevQuery || {}),
      exclude: {
        ...(prevQuery.exclude || {}),
        [filterKey]: value
      }
    };

    if (allowExcludeWithPartialMatch) {
      let partialMatchData = get(prevQuery, ['partial_match', partialKey ?? filterKey], '');
      if (partialMatchData) {
        let filterKeyNewValue = {
          "partial_match": {
            [partialKey ?? filterKey]: {
              ...partialMatchData
            }
          }
        };
        newFilters = {
          ...(prevQuery || {}),
          exclude: {
            ...(prevQuery.exclude || {}),
            ...filterKeyNewValue
          }
        };
        unset(newFilters, ['exclude', filterKey]);
      }
    }

    unset(newFilters, [filterKey]);
    unset(newFilters, ["partial_match", partialKey ?? type]);
    if (Object.keys(get(newFilters, "partial_match", {})).length === 0) {
      unset(newFilters, ["partial_match"]);
    }
    return newFilters;
  } else {
    let excludePartialMatchData = get(prevQuery, ["exclude", "partial_match", partialKey ?? filterKey], {});
    if (excludePartialMatchData && allowExcludeWithPartialMatch) {
      newFilters = {
        ...(prevQuery || {}),
        ["partial_match"]: {
          ...(prevQuery?.partial_match || {}),
          [partialKey ?? filterKey] : excludePartialMatchData
        }
      };
      unset(newFilters, ["exclude", "partial_match", partialKey ?? filterKey]);
    } else {
      newFilters = {
        ...(prevQuery || {}),
        [filterKey]: value
      };
    }

    if (type in ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY) {
      const isValueTpeString = ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY[type].valueFormat === "string";
      newFilters = {
        ...(prevQuery || {}),
        [ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY[type].filterKey || filterKey]: value
          ? isValueTpeString
            ? ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY[type].activeState
            : [ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY[type].activeState]
          : isValueTpeString
          ? ""
          : []
      };
    }

    if (type === "stacks") {
      if (typeof newFilters?.stacks === "string") {
        set(newFilters, ["stacks"], [newFilters?.stacks]);
      }
      // We need to scan the value for custom fields.
      // and then update newFilters.custom_stacks accordingly.
      const customFields: string[] = [];
      const stacks = newFilters.stacks ?? [];
      stacks.forEach((stackKey: string) => {
        if (stackKey?.includes(CUSTOM_FIELD_PREFIX) || stackKey?.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)) {
          customFields.push(stackKey);
        }
      });

      if (customFields.length) {
        newFilters.custom_stacks = customFields;
      } else {
        delete newFilters.custom_stacks;
      }
    }

    unset(newFilters, ["exclude", filterKey]);
    if (Object.keys(newFilters.exclude || {}).length === 0) {
      unset(newFilters, ["exclude"]);
    }
    if (type === TICKET_CATEGORIZATION_SCHEMES_KEY) {
      unset(newFilters, [TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY]);
    }
    return newFilters;
  }
};

export const buildCustomQuery = (
  prevQuery: any,
  value: any,
  type: any,
  exclude?: boolean,
  customFieldsKey = "custom_fields"
) => {
  const wQuery = get(prevQuery, [customFieldsKey], {});
  const wExcludeQuery = get(prevQuery, ["exclude", customFieldsKey], {});
  let newFilters;

  if (exclude) {
    newFilters = {
      ...(prevQuery || {}),
      exclude: {
        ...(prevQuery.exclude || {}),
        [customFieldsKey]: sanitizeObject({
          ...wExcludeQuery,
          [type]: value
        })
      }
    };

    unset(newFilters, [customFieldsKey, type]);
    unset(newFilters, ["partial_match", type]);
    if (Object.keys(get(newFilters, "partial_match", {})).length === 0) {
      unset(newFilters, ["partial_match"]);
    }
    if (Object.keys(get(newFilters, customFieldsKey, {})).length === 0) {
      unset(newFilters, [customFieldsKey]);
    }
    return newFilters;
  } else {
    newFilters = {
      ...(prevQuery || {}),
      [customFieldsKey]: sanitizeObject({
        ...wQuery,
        [type]: value
      })
    };
    unset(newFilters, ["exclude", customFieldsKey, type]);
    if (Object.keys(newFilters.exclude || {}).length === 0) {
      unset(newFilters, ["exclude"]);
    }
    if (Object.keys(get(newFilters, customFieldsKey, {})).length === 0) {
      unset(newFilters, [customFieldsKey]);
    }
    return newFilters;
  }
};

export const buildExcludeQuery = (
  filters: any,
  excludeKey: string,
  exclude: boolean,
  customFieldsKey = "custom_fields",
  useMapping = true,
  partialKey?: string,
  selectMode?: string,
  otherFlagData?: any
) => {
  if (excludeKey.includes("customfield_") || excludeKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
    const filterVal = get(filters, [customFieldsKey, excludeKey], []);
    const excludeVal = get(filters, ["exclude", customFieldsKey, excludeKey], []);
    return buildCustomQuery(filters, [...filterVal, ...excludeVal], excludeKey, exclude, customFieldsKey);
  } else {
    let filterKey = excludeKey;
    if (useMapping) {
      filterKey = get(valuesToFilters, [excludeKey], excludeKey);
    }
    const filterVal = get(filters, [filterKey], []);
    const excludeVal = get(filters, ["exclude", filterKey], []);
    return buildWidgetQuery(
      filters,
      selectMode === "default" ? filterVal : [...filterVal, ...excludeVal],
      excludeKey,
      exclude,
      customFieldsKey,
      useMapping,
      partialKey,
      otherFlagData
    );
  }
};

export const buildInitialReportQuery = (data: any) => {
  const { report, globalApplicationFilters, metadata: initialMetadata, dashboard, profileType } = data;
  let query: any = {};
  let metadata = initialMetadata;

  query["across"] = get(widgetConstants, [report, "defaultAcross"], "");

  const hasStacks = get(widgetConstants, [report, "stack_filters"], []).length > 0;
  if (hasStacks) {
    query["stacks"] = get(widgetConstants, [report, "defaultStacks"], []);
  }

  const defaultQuery = get(widgetConstants, [report, "default_query"], {});
  const _defaultQuery = typeof defaultQuery === "function" ? defaultQuery({ dashboard, profileType }) : defaultQuery;
  if (Object.keys(_defaultQuery).length > 0) {
    query = { ...query, ..._defaultQuery };
  }

  const defaultSettings = get(widgetConstants, [report, "default_settings"], {});
  if (Object.keys(defaultSettings).length > 0) {
    query = { ...query, ...defaultSettings };
  }

  query = { ...query, ...get(widgetConstants, [report, "defaultFilters"], {}) };

  if (globalApplicationFilters) {
    const sFilter = get(widgetConstants, [report, "supported_filters"], {});
    const sFilterURI = getSupportedFilterURI(sFilter);
    const uriSpecificFilter = get(globalApplicationFilters, [sFilterURI], undefined);
    const newMetadata = get(globalApplicationFilters, ["metadata", sFilterURI], undefined);
    if (uriSpecificFilter) {
      query = appendAndUpdateFilters(query, uriSpecificFilter);
    }
    if (newMetadata) {
      metadata = { ...(metadata || {}), ...(newMetadata || {}) };
    }
  }

  return { query, metadata };
};

export const buildPartialQuery = (
  filters: any,
  key: string,
  value: any,
  report: string,
  customFieldsKey = "custom_fields",
  otherFlagData: any = {}
) => {
  const { allowExcludeWithPartialMatch } = otherFlagData;
  let newFilters = filters;
  const filterKey = get(valuesToFilters, [key], key);
  const prevValue = ADDITIONAL_KEY_FILTERS.includes(key) ? "" : get(filters, [filterKey], undefined);
  const filterMapping = get(widgetConstants, [report, PARTIAL_FILTER_MAPPING_KEY], {});
  const partialFilterKey = get(widgetConstants, [report, PARTIAL_FILTER_KEY], JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
  const mappedKey = filterMapping?.[key] || key;
  let filterValue = value;
  let error: undefined | string = undefined;
  if (prevValue && Array.isArray(prevValue) && prevValue.length) {
    if (prevValue.length === 1 && value && (Object.values(value)[0] as any).length === 0) {
      filterValue = { [Object.keys(value)[0]]: prevValue[0] };
    } else if (prevValue.length > 1) {
      error = "Field can have only one value when filtering on a partial string.";
    }
  }
  if (!!filterValue) {
    let excludePartialMatchData = get(newFilters, ["exclude", partialFilterKey, mappedKey], undefined)
    if (excludePartialMatchData && allowExcludeWithPartialMatch) {
      newFilters = {
        ...newFilters,
        ['exclude']: {
          [partialFilterKey]: {
            ...(newFilters?.exclude?.[partialFilterKey] || {}),
            [mappedKey]: filterValue
          }
        }
      };
      unset(newFilters, ['partial_match', mappedKey]);
    } else {
      newFilters = {
        ...newFilters,
        [partialFilterKey]: {
          ...(newFilters?.[partialFilterKey] || {}),
          [mappedKey]: filterValue
        }
      };
    }
    unset(newFilters, [customFieldsKey, mappedKey]);
    unset(newFilters, ["exclude", filterKey]);
  } else {
    let excludePartialMatchData = get(newFilters, ["exclude", "partial_match", mappedKey ?? filterKey], {});
    if(allowExcludeWithPartialMatch && Object.keys(excludePartialMatchData).length > 0){
      newFilters = {
        ...newFilters,
        ["exclude"]: {
          ...(newFilters?.["exclude"] || {}),
          [filterKey]: [excludePartialMatchData?.[STARTS_WITH]] ?? [excludePartialMatchData?.[CONTAINS]]
        }
      }
      unset(newFilters, ["exclude", "partial_match", mappedKey ?? filterKey]);
      if (Object.keys(newFilters.exclude?.partial_match || {}).length === 0) {
        unset(newFilters, ["exclude", "partial_match"]);
      }
    }
    unset(newFilters, [partialFilterKey, mappedKey]);
    if (mappedKey.includes("customfield") || mappedKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      set(newFilters, [customFieldsKey, mappedKey], []);
    }
    if(!allowExcludeWithPartialMatch) unset(newFilters, ["exclude", filterKey]);
  }
  unset(newFilters, [filterKey]); 
  return { filters: newFilters, error };
};