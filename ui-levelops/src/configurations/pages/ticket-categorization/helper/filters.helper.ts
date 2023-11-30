import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalCustomSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCustomSprintFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { extractFilterAPIData, genericGetFilterAPIData } from "dashboard/report-filters/helper";
import { getFilterValue } from "helper/widgetFilter.helper";
import { uniqBy, get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import {
  WithSwitchProps,
  switchWithDropdownProps,
  withDeleteProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { isSanitizedValue } from "utils/commonUtils";

/**
 * @param application : jira or azure in this case
 * @returns config for custom field api calls
 */
export const getEICustomDataParams = (application: string) => {
  switch (application) {
    case "jira":
      return {
        fieldUri: "jira_fields",
        fieldId: `effort_${application}_fields`,
        integConfigUri: "jira_integration_config",
        integConfigId: `effort_${application}_integConfig`
      };
    case "azure_devops":
      return {
        fieldUri: "issue_management_workItem_Fields_list",
        fieldId: `effort_${application}_fields`,
        integConfigUri: "jira_integration_config",
        integConfigId: `effort_${application}_integConfig`
      };
    default:
      return {
        fieldUri: "jira_fields",
        fieldId: `effort_${application}_fields`,
        integConfigUri: "jira_integration_config",
        integConfigId: `effort_${application}_integConfig`
      };
  }
};

export const getEIAzureCustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
) => {
  return uniqBy(
    customData.map((custom: any) => {
      let isTimeBased = false;
      const itemFromFieldsList = (fieldsList || []).find(
        (item: { key: string; type: string; name: string }) => item.key === custom.key
      );
      if (itemFromFieldsList) {
        isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.type);
      }
      const transformedPrefix = get(custom, ["metadata", "transformed"]);
      const fieldKey = !!transformedPrefix ? custom?.key?.replace(transformedPrefix, "") : custom?.key;
      return baseFilterConfig(custom.key, {
        renderComponent:
          ((custom.name || "") as string).toLowerCase() === "sprint"
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
        excludeSupport: true,
        partialKey: custom.key,
        apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
        filterMetaData: {
          selectMode: "multiple",
          uri: "issue_management_custom_field_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [fieldKey],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          specialKey: custom.key,
          options: (args: any) => {
            const data = extractFilterAPIData(args, fieldKey);
            const newData = get(data, ["records"], []);
            return (newData as Array<any>)?.map((item: any) => ({
              label: item.key,
              value: item.key
            }));
          },
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      });
    }),
    "beKey"
  );
};

export const getEICustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
) => {
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
        apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
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
