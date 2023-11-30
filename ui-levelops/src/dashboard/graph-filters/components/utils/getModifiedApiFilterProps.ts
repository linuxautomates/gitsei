import { isExcludeVal } from "configurable-dashboard/helpers/helper";
import { get, uniqBy } from "lodash";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { FILTER_NAME_MAPPING, FILTER_PARENT_AND_VALUE_KEY } from "dashboard/constants/filter-name.mapping";
import { valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from 'dashboard/constants/widgetConstants'
import { PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { toTitleCase } from "utils/stringUtils";
import { stringSortingComparator } from "../sort.helper";

export const getModifiedApiFiltersProps = (
  item: any,
  props: any,
  partialFilterKeyMappings: any,
  useGlobalFilters: boolean = false,
  widgetValuesToFilterMapping = {}
) => {
  let apiFilterProps;

  if (item && props) {
    const { filters, reportType } = props;
    const getFiletParentKeyAndValueKey = get(widgetConstants, [reportType, FILTER_PARENT_AND_VALUE_KEY], undefined);
    const { parentKey, valueKey } = getFiletParentKeyAndValueKey(item);
    let dataKey = Object.keys(item)[0];
    let selectName = dataKey.replace(/_/g, " ");
    //@ts-ignore
    let value = get(filters, [parentKey, valuesToFilters[valueKey]]);
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

    let switchValue = isExcludeVal(filters, dataKey, "custom_fields", widgetValuesToFilterMapping);
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

    const withSwitchConfig = {
      showSwitchText: props.supportExcludeFilters,
      switchText: "Exclude",
      showSwitch: props.supportExcludeFilters,
      switchValue: switchValue,
      onSwitchValueChange: (value: any) => props.handleSwitchValueChange(dataKey, value)
    };

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
