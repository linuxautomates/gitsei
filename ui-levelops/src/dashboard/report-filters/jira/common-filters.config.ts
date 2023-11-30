import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CONTAINS, STARTS_WITH } from "dashboard/constants/constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalCustomSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCustomSprintFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import {
  JIRA_COMMON_FILTER_LABEL_MAPPING,
  JIRA_FILTER_KEY_MAPPING,
  JIRA_PARTIAL_FILTER_KEY_MAPPING
} from "dashboard/reports/jira/constant";
import { getFilterValue } from "helper/widgetFilter.helper";
import { get, uniqBy } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import {
  switchWithDropdownProps,
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { isSanitizedValue } from "utils/commonUtils";
import { toTitleCase } from "utils/stringUtils";
import { extractFilterAPIData, genericGetFilterAPIData } from "../helper";

export const COMMON_ACROSS_OPTIONS = [
  { label: "Affects Version", value: "version" },
  { label: "ASSIGNEE", value: "assignee" },
  { label: "COMPONENT", value: "component" },
  { label: "FIX VERSION", value: "fix_version" },
  { label: "Issue Created", value: "issue_created" },
  { label: "ISSUE TYPE", value: "issue_type" },
  { label: "Issue Updated", value: "issue_updated" },
  { label: "LABEL", value: "label" },
  { label: "PRIORITY", value: "priority" },
  { label: "PROJECT", value: "project" },
  { label: "REPORTER", value: "reporter" },
  { label: "RESOLUTION", value: "resolution" },
  { label: "STATUS", value: "status" },
  { label: "STATUS CATEGORY", value: "status_category" }
];

const commonJiraFilters = jiraSupportedFilters.values
  .filter((filter: string) => filter !== "project")
  .map((filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  }));

export const jiraCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig(JIRA_FILTER_KEY_MAPPING["project"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Projects",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "none",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "project",
    supportPaginatedSelect: true,
    apiFilterProps: genericApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "jiraprojects_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["project_name"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const data = extractFilterAPIData(args, "project_name");
        return data?.map((item: any) => ({
          label: `${toTitleCase(item.additional_key)} (${item.key})`,
          value: item.key
        }));
      },
      specialKey: "project",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...commonJiraFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((JIRA_FILTER_KEY_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: JIRA_COMMON_FILTER_LABEL_MAPPING[item.label.toLowerCase()] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: JIRA_PARTIAL_FILTER_KEY_MAPPING[item.key] ?? item.key,
      supportPaginatedSelect: true,
      apiFilterProps: genericApiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "jira_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        options: (args: any) => {
          const data = extractFilterAPIData(args, item.key);
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];

export const generateCustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
): LevelOpsFilter[] => {
  return uniqBy(
    customData.map((custom: any) => {
      const isCustomSprint = ((custom.name || "") as string).toLowerCase() === "sprint";
      let isTimeBased = false;
      const itemFromFieldsList = (fieldsList || []).find(
        (item: { key: string; type: string; name: string }) => item.key === custom.key
      );
      if (itemFromFieldsList) {
        isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.type);
      }
      return baseFilterConfig(custom.key, {
        renderComponent: isCustomSprint
          ? UniversalCustomSprintSelectFilterWrapper
          : isTimeBased
          ? UniversalTimeBasedFilter
          : UniversalSelectFilterWrapper,
        apiContainer: isTimeBased ? undefined : APIFilterContainer,
        label: custom.name,
        deleteSupport: true,
        supportPaginatedSelect: true,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        partialSupport: true,
        excludeSupport: true,
        partialKey: custom.key,
        isSelected: isCustomSprint
          ? (args: any) => {
              const { filters } = args || {};
              const hasValue = isSanitizedValue(getFilterValue(filters, custom.key, true, undefined)?.value);
              const hasLastSprint = !!filters?.last_sprint;
              const hasSprintStates = !!filters?.sprint_states || !!filters?.jira_sprint_states;
              if (hasValue || hasLastSprint || hasSprintStates) {
                return true;
              }
              return false;
            }
          : undefined,
        apiFilterProps: (args: any) => {
          const partialValue = get(
            args.allFilters,
            [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
            {}
          );
          const switchValue = !!get(
            args?.allFilters || {},
            ["exclude", "custom_fields", args?.excludeKey ?? args?.beKey],
            undefined
          );
          const selectPropOptions = [
            {
              label: "Start With",
              value: STARTS_WITH
            },
            {
              label: "Contain",
              value: CONTAINS
            }
          ];
          const withSwitch: WithSwitchProps = {
            showSwitch: isCustomSprint ? false : true,
            showSwitchText: true,
            switchText: "Exclude",
            switchValue,
            onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value)
          };
          const switchWithDropdown: switchWithDropdownProps = {
            showSwitchWithDropdown: true,
            checkboxProps: {
              text: "Include all values that",
              disabled: switchValue,
              value: Object.keys(partialValue).length > 0
            },
            selectProps: {
              options: selectPropOptions,
              value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : selectPropOptions[0].value,
              disabled: switchValue || !(Object.keys(partialValue).length > 0),
              onSelectChange: (key: any) =>
                args?.handlePartialValueChange?.(
                  args?.partialKey ?? args?.beKey,
                  key ? { [key]: Object.values(partialValue)[0] || "" } : undefined
                )
            }
          };
          const withDelete: withDeleteProps = {
            showDelete: args?.deleteSupport,
            key: args?.beKey,
            onDelete: args.handleRemoveFilter
          };
          return { withSwitch, switchWithDropdown, withDelete };
        },
        filterMetaData: {
          selectMode: "multiple",
          uri: "jira_custom_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [custom.key],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => genericGetFilterAPIData(args, custom.key),
          specialKey: custom.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      });
    }),
    "beKey"
  );
};
