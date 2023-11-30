import { get, uniqBy } from "lodash";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { jiraZenDeskSupportedFilters } from "dashboard/constants/supported-filters.constant";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalCustomSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCustomSprintFilter";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { getFilterValue } from "helper/widgetFilter.helper";
import { isSanitizedValue } from "utils/commonUtils";

const commonJiraZendeskFilters: Array<{ key: string; label: string }> = jiraZenDeskSupportedFilters.values.map(
  (filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  })
);

export const jiraZendeskFilterKeyMapping: basicMappingType<string> = {
  jira_status: "jira_statuses",
  jira_priority: "jira_priorities",
  jira_assignee: "jira_assignees",
  jira_project: "jira_projects",
  jira_issue_type: "jira_issue_types",
  jira_component: "jira_components",
  jira_label: "jira_labels",
  zendesk_brand: "zendesk_brands",
  zendesk_type: "zendesk_types",
  zendesk_priority: "zendesk_priorities",
  zendesk_status: "zendesk_statuses",
  zendesk_organization: "zendesk_organizations",
  zendesk_assignee: "zendesk_assignees",
  zendesk_requester: "zendesk_requesters",
  zendesk_submitter: "zendesk_submitters"
};

const apiFilterProps = (args: any) => {
  const withDelete: withDeleteProps = {
    showDelete: args?.deleteSupport,
    key: args?.beKey,
    onDelete: args.handleRemoveFilter
  };
  return { withDelete };
};

export const jiraZendeskCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonJiraZendeskFilters
    .filter(item => item.key.includes("zendesk_"))
    .map((item: { key: string; label: string }) =>
      baseFilterConfig(jiraZendeskFilterKeyMapping[item?.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: item?.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: false,
        excludeSupport: false,
        supportPaginatedSelect: true,
        apiFilterProps,
        filterMetaData: {
          selectMode: "multiple",
          uri: "zendesk_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [item.key.replace("zendesk_", "")],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const filterMetaData = get(args, ["filterMetaData"], {});
            const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
            const currData = filterApiData.find(
              (fData: any) => Object.keys(fData)[0] === item.key.replace("zendesk_", "")
            );
            if (currData) {
              return (Object.values(currData)[0] as Array<any>)
                ?.map((item: any) => ({
                  label: item.additional_key ?? item.key,
                  value: item.key
                }))
                .filter((item: { label: string; value: string }) => !!item.value);
            }
            return [];
          },
          specialKey: item.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      })
    ),

  ...commonJiraZendeskFilters
    .filter(item => item.key.includes("jira_"))
    .map((item: { key: string; label: string }) =>
      baseFilterConfig(jiraZendeskFilterKeyMapping[item?.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: item?.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: false,
        excludeSupport: false,
        supportPaginatedSelect: true,
        apiFilterProps,
        filterMetaData: {
          selectMode: "multiple",
          uri: "jira_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [item.key.replace("jira_", "")],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const filterMetaData = get(args, ["filterMetaData"], {});
            const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
            const currData = filterApiData.find(
              (fData: any) => Object.keys(fData)[0] === item.key.replace("jira_", "")
            );
            if (currData) {
              return (Object.values(currData)[0] as Array<any>)
                ?.map((item: any) => ({
                  label: item.additional_key ?? item.key,
                  value: item.key
                }))
                .filter((item: { label: string; value: string }) => !!item.value);
            }
            return [];
          },
          specialKey: item.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      })
    )
];

export const generateZendeskCustomFieldConfig = (
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
        partialSupport: false,
        excludeSupport: false,
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
        apiFilterProps,
        filterMetaData: {
          selectMode: "multiple",
          uri: "zendesk_custom_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [custom.key],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const filterMetaData = get(args, ["filterMetaData"], {});
            const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
            const currData = filterApiData.find((item: any) => Object.keys(item)[0] === custom.key);
            if (currData) {
              return (Object.values(currData)[0] as Array<any>)?.map((item: any) => ({
                label: item.key,
                value: item.key
              }));
            }
            return [];
          },
          specialKey: custom.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      });
    }),
    "beKey"
  );
};
