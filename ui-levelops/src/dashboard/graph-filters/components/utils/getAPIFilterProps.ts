import { getFilterOptions, getFilterValue, isExcludeVal } from "configurable-dashboard/helpers/helper";
import { get, uniqBy } from "lodash";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants"
import {
  PARTIAL_FILTER_KEY,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  DISABLE_EXCLUDE_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { toTitleCase } from "utils/stringUtils";
import { stringSortingComparator } from "../sort.helper";

export const getAPIFilterProps = (
  item: any,
  props: any,
  partialFilterKeyMappings: any,
  useGlobalFilters: boolean = false,
  widgetValuesToFilterMapping = {}
) => {
  let apiFilterProps;

  if (item && props) {
    const { filters, reportType } = props;
    let dataKey = Object.keys(item)[0]; // model, project
    let selectName = dataKey.replace(/_/g, " ");
    let value = getFilterValue(
      filters,
      dataKey,
      props.fromEIProfileFlow !== undefined ? props.fromEIProfileFlow : false,
      widgetValuesToFilterMapping
    );
    let options = uniqBy([...(item[dataKey] || [])], "key");
    options = options.filter((item: any) => !!item?.key);
    //converting option into upper case so we can use it both the places (APIFilterManyOptions, CustomSelect)
    if (!dataKey.includes("@")) {
      options = options
        .map((option: any) => ({
          key: option.key,
          value: option?.value ? option.value : toTitleCase(option.key)
        }))
        .sort(stringSortingComparator("value"));
    }
    if (dataKey.includes("@")) {
      const customKeys = dataKey.split("@");
      options = getFilterOptions(item[dataKey] || []).sort(stringSortingComparator("label"));
      dataKey = customKeys[0];
      selectName = customKeys[1].replace(/_/g, " ");
      value = getFilterValue(filters, dataKey, props.fromEIProfileFlow !== undefined ? props.fromEIProfileFlow : false);
    }
    let switchValue = isExcludeVal(
      filters,
      dataKey,
      props.fromEIProfileFlow === true && dataKey.includes(AZURE_CUSTOM_FIELD_PREFIX)
        ? "workitem_custom_fields"
        : "custom_fields",
      widgetValuesToFilterMapping
    );
    const partialFilterKey = get(widgetConstants, [reportType, PARTIAL_FILTER_KEY], JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
    let partialValue = get(filters, [partialFilterKey, partialFilterKeyMappings?.[dataKey] || dataKey], {});

    // version changed to Affects Version for jira reports
    if (Object.keys(widgetConstants).includes(reportType)) {
      const mapping = getWidgetConstant(reportType, FILTER_NAME_MAPPING);
      selectName = get(mapping, [dataKey], selectName);
    }

    if (useGlobalFilters) {
      const uri = get(widgetConstants, [reportType, "supported_filters", "uri"], "");
      if (
        ["jenkins_jobs_filter_values", "cicd_filter_values", "jenkins_pipelines_jobs_filter_values"].includes(uri) &&
        selectName === "job normalized full names"
      ) {
        selectName = "JENKINS JOB PATH";
      }
    }

    const excludeFilterDisableKeys = get(widgetConstants, [reportType, DISABLE_EXCLUDE_FILTER_MAPPING_KEY], []);

    const withSwitchConfig = {
      showSwitchText: props.supportExcludeFilters,
      switchText: "Exclude",
      showSwitch: (excludeFilterDisableKeys || []).length
        ? !excludeFilterDisableKeys.includes(dataKey)
        : props.supportExcludeFilters,
      switchValue: switchValue,
      onSwitchValueChange: (value: any) => props.handleSwitchValueChange(dataKey, value)
    };

    // Below changes will get removed once BE added partial match support for velocity configs
    if (props?.customProps?.label) {
      selectName = props?.customProps?.label;
      dataKey = filters?.[props?.customProps?.partialMatchKey]
        ? props?.customProps?.partialMatchKey
        : props?.customProps?.key;
      partialValue =
        dataKey === props?.customProps?.partialMatchKey
          ? { [dataKey]: filters?.[dataKey] ? filters?.[dataKey] : [] }
          : {};
      value = filters[dataKey];
    }
    apiFilterProps = {
      dataKey,
      selectName,
      value,
      options,
      switchValue,
      partialValue,
      withSwitchConfig
    };
  }

  return apiFilterProps;
};
