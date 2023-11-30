import { upperCase, cloneDeep, get, unset } from "lodash";
import { complexFilterKeys, filesFilters, supportedFiltersLabelMapping } from "./filterConstants";
import { valuesToFilters } from "dashboard/constants/constants";
import { getApplicationFilters } from "../helper";
import { getGroupByRootFolderKey } from "../../../../configurable-dashboard/helpers/helper";

export const help = "help";

export const getInitialOptions = (
  supportedFilters: string[],
  customFilters: { [key: string]: any[] }[],
  uiFilters: { key: string; defaultValue: any; label?: string }[],
  filters: any,
  uri: string
) => {
  let list: { key: string; selected: boolean; defaultValue: any; label: string }[] = [];
  if (supportedFilters.length) {
    supportedFilters.forEach(key =>
      list.push({
        key: get(supportedFiltersLabelMapping, [uri, key, "key"], undefined) || (valuesToFilters as any)[key] || key,
        label: get(supportedFiltersLabelMapping, [uri, key, "label"], undefined) || upperCase(key.replace(/_/g, " ")),
        selected: false,
        defaultValue: []
      })
    );
  }

  if (customFilters && customFilters.length) {
    customFilters.forEach((filter: any) =>
      list.push({
        key: filter.key,
        label: !!filter?.name ? upperCase(filter.name.replace(/_/g, " ")) : filter?.key,
        selected: false,
        defaultValue: []
      })
    );
  }

  if (uiFilters && uiFilters.length) {
    uiFilters.forEach(filter =>
      list.push({
        key: filter.key,
        label: filter.label ? filter.label : upperCase(filter.key.replace(/_/g, " ")),
        selected: false,
        defaultValue: filter.defaultValue
      })
    );
  }

  let _filterKeys = Object.keys(filters);

  if (_filterKeys.includes("exclude")) {
    _filterKeys = [..._filterKeys, ...Object.keys(filters.exclude || {})];
  }

  if (_filterKeys.includes("custom_fields")) {
    _filterKeys = [..._filterKeys, ...Object.keys(filters.custom_fields || {})];
  }

  if (_filterKeys.includes("partial_match")) {
    _filterKeys = [
      ..._filterKeys,
      ...Object.keys(filters.partial_match).map(key => (valuesToFilters as any)[key] || key)
    ];
  }

  if (filters.hasOwnProperty("workitem_attributes") && Object.keys(filters?.workitem_attributes).length > 0) {
    if (filters?.workitem_attributes.hasOwnProperty("teams") && filters?.workitem_attributes?.teams.length > 0) {
      _filterKeys = [..._filterKeys, "teams"];
    }
    if (
      filters?.workitem_attributes.hasOwnProperty("code_area") &&
      filters?.workitem_attributes?.code_area.length > 0
    ) {
      _filterKeys = [..._filterKeys, "code_area"];
    }
  }

  _filterKeys = _filterKeys.filter(key => !complexFilterKeys.includes(key));

  list = list.map(item => ({
    ...item,
    selected: _filterKeys.includes(item.key)
  }));

  return list;
};

export const removeFilterKey = (filters: any, key: string, isCustomSprint?: boolean, prefixPath?: string, getExcludeWithPartialMatchKeyFlag?: boolean, partialKey?: string) => {
  const _filters = cloneDeep({ ...filters });
  const _key = (valuesToFilters as any)[key] || key;

  if (Object.keys(_filters).includes(_key)) {
    delete _filters[_key];
  }

  if (Object.keys(_filters).includes(key)) {
    delete _filters[key];
  }

  if (_filters.exclude && Object.keys(_filters.exclude).includes(_key)) {
    delete _filters.exclude[_key];
  }

  if (getExcludeWithPartialMatchKeyFlag && partialKey && _filters.exclude?.partial_match && Object.keys(_filters.exclude?.partial_match).includes(partialKey)) {
    delete _filters.exclude?.partial_match[partialKey];
    if (Object.keys(_filters.exclude?.partial_match || {}).length === 0) {
      delete _filters.exclude?.partial_match;
    }
  }

  if (_filters.custom_fields && Object.keys(_filters.custom_fields).includes(_key)) {
    delete _filters.custom_fields[_key];
  }

  // Added this logic coz key and _key both represent valuesToFilters keys(already plural)
  // in case of partial fields so need to convert each partial key to plural for comparision
  if (_filters.partial_match && Object.keys(_filters.partial_match)) {
    Object.keys(_filters.partial_match).forEach(field => {
      let field_key = (valuesToFilters as any)[field] || field;
      if (field_key === key) {
        delete _filters.partial_match[field];
      }
    });
  }

  if (
    _filters.exclude &&
    _filters.exclude.custom_fields &&
    Object.keys(_filters.exclude.custom_fields).includes(_key)
  ) {
    delete _filters.exclude.custom_fields[_key];
  }

  if (_filters.missing_fields && Object.keys(_filters.missing_fields)) {
    Object.keys(_filters.missing_fields).forEach(field => {
      let field_key = (valuesToFilters as any)[field] || field;
      if (field_key === key) {
        delete _filters.missing_fields[field];
      }
    });
  }

  if (isCustomSprint) {
    delete _filters["last_sprint"];
    delete _filters["jira_sprint_states"];
  }

  if (
    Object.keys(_filters || {}).includes(prefixPath || "") &&
    Object.keys(_filters?.[prefixPath as string] || {}).includes(_key)
  ) {
    unset(_filters, [prefixPath ?? "", key]);
  }

  return { ..._filters };
};

export const removeChildFilterKey = (filters: any, parentKey: string, childKey: string) => {
  const _filters = cloneDeep({ ...filters });
  const _childKey = (valuesToFilters as any)[childKey] || childKey;

  if (Object.keys(_filters).includes(parentKey) && Object.keys(_filters[parentKey]).includes(_childKey)) {
    delete _filters[parentKey][_childKey];
  }

  if (
    _filters.exclude &&
    Object.keys(_filters.exclude).includes(parentKey) &&
    Object.keys(_filters.exclude[parentKey]).includes(_childKey)
  ) {
    delete _filters.exclude[parentKey][_childKey];
  }

  if (
    _filters.custom_fields &&
    Object.keys(_filters.custom_fields).includes(parentKey) &&
    Object.keys(_filters.custom_fields[parentKey]).includes(_childKey)
  ) {
    delete _filters.custom_fields[parentKey][_childKey];
  }

  // Added this logic coz key and _key both represent valuesToFilters keys(already plural)
  // in case of partial fields so need to convert each partial key to plural for comparision
  if (_filters.partial_match && Object.keys(_filters.partial_match)) {
    Object.keys(_filters.partial_match).forEach(field => {
      let field_key = (valuesToFilters as any)[field] || field;
      if (field_key === _childKey) {
        delete _filters.partial_match[field];
      }
    });
  }

  if (
    _filters.exclude &&
    _filters.exclude.custom_fields &&
    Object.keys(_filters.exclude.custom_fields).includes(parentKey) &&
    Object.keys(_filters.exclude.custom_fields[parentKey]).includes(_childKey)
  ) {
    delete _filters.exclude.custom_fields[parentKey][_childKey];
  }

  if (_filters.missing_fields && Object.keys(_filters.missing_fields)) {
    Object.keys(_filters.missing_fields).forEach(field => {
      let field_key = (valuesToFilters as any)[field] || field;
      if (field_key === _childKey) {
        delete _filters.missing_fields[field];
      }
    });
  }

  return { ..._filters };
};

export const getOrderedFilterKeys = (filters: { [key: string]: any }, supportedFilters: string[]) => {
  let keys = Object.keys(filters)
    .filter(key => !complexFilterKeys.includes(key) && supportedFilters.includes(key))
    .map(_key => (valuesToFilters as any)[_key] || _key);

  if (filters.hasOwnProperty("workitem_attributes") && Object.keys(filters?.workitem_attributes).length > 0) {
    if (filters?.workitem_attributes.hasOwnProperty("teams") && filters?.workitem_attributes?.teams.length > 0) {
      keys = [...keys, "teams"];
    }
    if (
      filters?.workitem_attributes.hasOwnProperty("code_area") &&
      filters?.workitem_attributes?.code_area.length > 0
    ) {
      keys = [...keys, "code_area"];
    }
  }

  if (filters.partial_match && Object.keys(filters.partial_match).length > 0) {
    keys = [...keys, ...Object.keys(filters.partial_match).map(key => (valuesToFilters as any)[key] || key)];
  }

  if (filters.exclude && Object.keys(filters.exclude).length > 0) {
    keys = [
      ...keys,
      ...Object.keys(filters.exclude).filter(key => key !== "custom_fields" && key !== "workitem_custom_fields")
    ];

    if (filters.exclude.custom_fields && Object.keys(filters.exclude.custom_fields).length > 0) {
      keys = [...keys, ...Object.keys(filters.exclude.custom_fields)];
    }

    if (filters.exclude.workitem_custom_fields && Object.keys(filters.exclude.workitem_custom_fields).length > 0) {
      keys = [...keys, ...Object.keys(filters.exclude.workitem_custom_fields)];
    }
  }

  if (filters.custom_fields && Object.keys(filters.custom_fields).length > 0) {
    keys = [...keys, ...Object.keys(filters.custom_fields)];
  }

  if (filters.workitem_custom_fields && Object.keys(filters.workitem_custom_fields).length > 0) {
    keys = [...keys, ...Object.keys(filters.workitem_custom_fields)];
  }

  keys = [
    ...keys,
    ...Object.keys(filters).filter(key => !complexFilterKeys.includes(key) && !supportedFilters.includes(key))
  ];

  return keys;
};

export const getGlobalOrderedFilterKeys = (filters: { [key: string]: any }) => {
  return Object.keys(filters).reduce((acc: any, uri: string) => {
    const supportedFilter: string[] = get(getApplicationFilters(), [uri, "filters", "values"], []);

    if (filesFilters.includes(uri as any)) {
      let _ordered_filters = getOrderedFilterKeys(filters[uri], supportedFilter);

      if (filters.metadata?.[uri] && filters.metadata?.[uri].hasOwnProperty(getGroupByRootFolderKey(uri))) {
        _ordered_filters.push("group_by_modules");
      }
      return {
        ...acc,
        [uri]: [..._ordered_filters]
      };
    }

    return {
      ...acc,
      [uri]: getOrderedFilterKeys(filters[uri], supportedFilter)
    };
  }, {});
};

// Used in AddFilters for getting supported filters
export const getReportFilter = (filter: { uri: string; values: string[] }) => {
  let _filter = filter;
  if (["praetorian_issues_values", "ncc_group_issues_values"].includes(filter.uri)) {
    const values =
      filter.uri === "ncc_group_issues_values" ? ["component", "risk", "category"] : ["priority", "category"];
    _filter = {
      uri: filter.uri,
      values: values
    };
    return _filter;
  } else {
    return _filter;
  }
};
