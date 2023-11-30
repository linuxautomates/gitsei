import { INTEGRATION_FIELD_NUMERIC_TYPES } from "configurations/containers/integration-steps/constant";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { issueManagementCommonFilterOptionsMapping } from "dashboard/constants/filter-name.mapping";
import { hygieneTypes } from "dashboard/constants/hygiene.constants";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import AzureCodeAreaFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/AzureCodeAreaFilter/AzureCodeAreaFilter.container";
import AzureIterationFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/AzureIterationFilter/AzureIterationFilter.container";
import { AzureTeamFilterContainer } from "dashboard/graph-filters/components/GenericFilterComponents/AzureTeamFilter/AzureTeamFilterContainer";
import ExcludeStatusAPIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/ExcludeStatusApiContainer";
import UniversalCustomSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCustomSprintFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { extractFilterAPIData } from "dashboard/report-filters/azure/helper";
import genericApiFilterProps, {
  genericCustomFieldApiProps,
  withDeleteAPIProps,
  WithSwitchFilterProps
} from "dashboard/report-filters/common/common-api-filter-props";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { generateStoryPointsFilterConfig } from "dashboard/report-filters/common/story-points-filters.config";
import { get, uniqBy } from "lodash";
import {
  ApiDropDownData,
  baseFilterConfig,
  DropDownData,
  LevelOpsFilter,
  LevelOpsFilterTypes
} from "model/filters/levelopsFilters";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { toTitleCase } from "utils/stringUtils";
import { AZURE_FILTER_KEY_MAPPING, AZURE_PARTIAL_FILTER_KEY_MAPPING } from "./constant";
import {
  apiFilterProps as azureIterationApiFilterProps,
  getMappedValue as azureIterationHetMappedValue,
  isSelected as azureIterationIsSelected
} from "./helpers/azure-iteration-filter.helper";

const commonAzureFilters = issueManagementSupportedFilters?.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const azureCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)["workitem_priority"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "WORKITEM PRIORITY",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "workitem_priority",
    supportPaginatedSelect: true,
    apiFilterProps: genericApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "issue_management_workitem_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["priority"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      specialKey: "workitem_priority",
      options: (args: any) => {
        const data = extractFilterAPIData(args, "priority");
        return (data as Array<any>)?.map((item: any) => ({
          label: get(staticPriorties, [item.key], item.key),
          value: item.key
        }));
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...commonAzureFilters
    .filter((item: { key: string; label: string }) => item.key !== "workitem_priority")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)[item.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: (issueManagementCommonFilterOptionsMapping as any)[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: true,
        excludeSupport: true,
        partialKey: AZURE_PARTIAL_FILTER_KEY_MAPPING[item.key] ?? item.key,
        supportPaginatedSelect: true,
        apiFilterProps: genericApiFilterProps,
        filterMetaData: {
          selectMode: "multiple",
          uri: "issue_management_workitem_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [newKey],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            const data = extractFilterAPIData(args, newKey);
            if (data) {
              return (data as Array<any>)?.map((item: any) => ({
                label: item.additional_key ?? item.key,
                value: item.key
              }));
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

export const generateAzureCustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
): LevelOpsFilter[] => {
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
        partialSupport: INTEGRATION_FIELD_NUMERIC_TYPES.includes(itemFromFieldsList?.type ?? "") ? false : true,
        excludeSupport: true,
        partialKey: custom.key,
        apiFilterProps: genericCustomFieldApiProps,
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
            return (data as Array<any>)?.map((item: any) => ({
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

export const WorkitemCreatedAtFilterConfig: LevelOpsFilter = {
  id: "workitem_created_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "WorkItem Created In",
  beKey: "workitem_created_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const WorkitemresolvedAtFilterConfig: LevelOpsFilter = {
  id: "workitem_resolved_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "WorkItem resolved In",
  beKey: "workitem_resolved_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const WorkitemUpdatedAtFilterConfig: LevelOpsFilter = {
  id: "workitem_updated_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "WorkItem Updated In",
  beKey: "workitem_updated_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const AzureTeamsFilterConfig: LevelOpsFilter = {
  id: "workitem_attributes_teams",
  renderComponent: AzureTeamFilterContainer,
  label: "AZURE TEAMS",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  beKey: "teams",
  parentKey: "workitem_attributes",
  type: LevelOpsFilterTypes.CUSTOM,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "teams",
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    selectMode: "multiple",
    options: [],
    sortOptions: true
  } as DropDownData
};

export const AzureCodeAreaFilterConfig: LevelOpsFilter = {
  id: "workitem_attributes_code_area",
  renderComponent: AzureCodeAreaFilterContainer,
  label: "AZURE AREAS",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  beKey: "code_area",
  parentKey: "workitem_attributes",
  type: LevelOpsFilterTypes.CUSTOM,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "workitem_attributes",
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    selectMode: "multiple",
    options: [],
    sortOptions: true
  } as DropDownData
};

export const AzureIterationFilterConfig: LevelOpsFilter = {
  id: "workitem_attributes_code_area",
  renderComponent: AzureIterationFilterContainer,
  label: "AZURE ITERATION",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  beKey: "azure_iteration",
  type: LevelOpsFilterTypes.CUSTOM,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "azure_iteration",
  supportPaginatedSelect: true,
  apiFilterProps: azureIterationApiFilterProps,
  getMappedValue: azureIterationHetMappedValue,
  isSelected: azureIterationIsSelected,
  filterMetaData: {
    activeIterationKey: "workitem_sprint_states"
  } as any
};

export const ExcludeStatusFilterConfig: LevelOpsFilter = {
  id: "stages",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: ExcludeStatusAPIFilterContainer,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  label: "Exclude Time in Status",
  beKey: "workitem_stages",
  labelCase: "title_case",
  filterInfo: "Exclude time spent in the selected Azure states from resolution time",
  deleteSupport: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    alwaysExclude: true,
    uri: "issue_management_workitem_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["status"],
        filter: { integration_ids: get(args, "integrationIds", []), status_categories: ["Done", "DONE"] }
      };
    },
    selectMode: "multiple",
    specialKey: "exclude_status",
    options: (args: any) => {
      const data = extractFilterAPIData(args, "status");
      return (data as Array<any>)?.map((item: any) => ({
        label: item.key,
        value: item.key
      }));
    },
    sortOptions: true,
    createOption: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const WorkitemHygieneFilterConfig: LevelOpsFilter = generateHygieneFilterConfig(
  hygieneTypes.map((item: string) => ({ label: item, value: item })),
  "Workitem Hygiene",
  "workitem_hygiene_types",
  false
);

export const WorkitemStoryPointsConfig: LevelOpsFilter = generateStoryPointsFilterConfig(
  "workitem_story_points",
  "AZURE STORY POINTS"
);

export const WorkitemTicketCategoryFilterConfig: LevelOpsFilter = baseFilterConfig(
  (AZURE_FILTER_KEY_MAPPING as any)["workitem_ticket_category"],
  {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Workitem Ticket Category",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "workitem_ticket_category",
    supportPaginatedSelect: true,
    apiFilterProps: genericApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "issue_management_workitem_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["ticket_category"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const data = extractFilterAPIData(args, "ticket_category");
        return (data as Array<any>)?.map((item: any) => ({
          label: item.additional_key ?? item.key,
          value: item.key
        }));
      },
      specialKey: "workitem_ticket_category",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }
);

export const ApplyFilterToNodeFilterConfig: LevelOpsFilter = {
  id: "apply_ou_on_velocity_report",
  renderComponent: UniversalSelectFilterWrapper,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  label: "Apply Filters",
  beKey: "apply_ou_on_velocity_report",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  defaultValue: true,
  filterMetaData: {
    options: [
      { value: true, label: "apply filters to all nodes" },
      { value: false, label: "apply filters only for the initial node" }
    ],
    sortOptions: true,
    clearSupport: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

/**
 * It takes an object with any number of properties, and returns an object with the same properties,
 * but with the properties of `ApplyFilterToNodeFilterConfig` added to it
 * @param args - Record<T, any>
 * @returns a LevelOpsFilter object.
 */
export function generateApplyFilterToNodeFilterConfig<T extends keyof LevelOpsFilter>(
  args: Record<T, any>
): LevelOpsFilter {
  return {
    ...ApplyFilterToNodeFilterConfig,
    ...args
  };
}

export const WorkitemFeaturesFilterConfig: LevelOpsFilter = {
  renderComponent: UniversalSelectFilterWrapper,
  beKey: "workitem_feature",
  id: "feature",
  apiContainer: APIFilterContainer,
  label: "Features",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.DROPDOWN,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: false,
  excludeSupport: false,
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    selectMode: "multiple",
    uri: "issue_management_list",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        filter: { integration_ids: get(args, "integrationIds", []), workitem_types: ["Feature"] }
      };
    },
    options: (args: any) => {
      const data = get(args, ["filterMetaData", "apiConfig", "data"]);
      if (data) {
        return (data as Array<any>)?.map((item: any) => ({
          label: item?.summary ?? item?.workitem_id,
          value: item?.workitem_id
        }));
      }
      return [];
    },
    specialKey: "feature",
    sortOptions: true,
    createOption: true
  } as ApiDropDownData
};

export const WorkitemUserStoryFilterConfig: LevelOpsFilter = {
  renderComponent: UniversalSelectFilterWrapper,
  beKey: "workitem_user_story",
  id: "user_story",
  apiContainer: APIFilterContainer,
  label: "User Stories",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.DROPDOWN,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: false,
  excludeSupport: false,
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  filterMetaData: {
    selectMode: "multiple",
    uri: "issue_management_list",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        filter: { integration_ids: get(args, "integrationIds", []), workitem_types: ["User Story"] }
      };
    },
    options: (args: any) => {
      const data = get(args, ["filterMetaData", "apiConfig", "data"]);
      if (data) {
        return (data as Array<any>)?.map((item: any) => ({
          label: item?.summary ?? item?.workitem_id,
          value: item?.workitem_id
        }));
      }
      return [];
    },
    specialKey: "user_story",
    sortOptions: true,
    createOption: true
  } as ApiDropDownData
};

export const MetricViewByFilterConfig: LevelOpsFilter = {
  id: "view_by",
  renderComponent: UniversalSelectFilterWrapper,
  label: "View By",
  beKey: "view_by",
  labelCase: "title_case",
  defaultValue: "Points",
  filterMetaData: {
    options: [
      { value: "Points", label: "Story Points" },
      { value: "Tickets", label: "tickets" }
    ],
    selectMode: "default"
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
