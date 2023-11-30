import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import ScmCodeChangeFiltersWrapper from "dashboard/graph-filters/components/GenericFilterComponents/ScmCodeChangeFilterWrapper";
import ScmCommentDensityFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/ScmCommentDensityFilterWrapper.tsx";
import { ScmOtherCriteriaWrapper } from "dashboard/graph-filters/components/GenericFilterComponents/ScmOtherCriteriaFilterWrapper";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import { cloneDeep, get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import {
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const CodeChangeSizeWrapperFilterConfig: LevelOpsFilter = {
  id: "code_change_size",
  renderComponent: ScmCodeChangeFiltersWrapper,
  beKey: "code_change_size",
  label: "Code Change Size",
  labelCase: "title_case",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const CodeDensityWrapperFilterConfig: LevelOpsFilter = {
  id: "comment_density",
  renderComponent: ScmCommentDensityFilterWrapper,
  beKey: "comment_density",
  label: "Pr Code Density",
  labelCase: "title_case",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const OtherCriteriaFilterConfig: LevelOpsFilter = {
  id: "approval_statuses",
  renderComponent: ScmOtherCriteriaWrapper,
  beKey: "approval_statuses",
  label: "Other Criteria",
  labelCase: "title_case",
  filterMetaData: {},
  isSelected: (args: any) => {
    const { filters } = args;
    return get(filters, ["approval_statuses"], []).length || !!(get(filters, ["linked_issues_key"], "false") != "false");
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const NumberOfReviewersFilterConfig: LevelOpsFilter = {
  id: "num_reviewers",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Number of Reviewers",
  beKey: "num_reviewers",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: true,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};

export const NumberOfApproversFilterConfig: LevelOpsFilter = {
  id: "num_approvers",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Number of Approvers",
  beKey: "num_approvers",
  labelCase: "none",
  filterMetaData: {},
  deleteSupport: true,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  }
};

export const PrCommentDensityFilterConfig: LevelOpsFilter = {
  id: "comment_densities",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Pr Comment Density",
  beKey: "comment_densities",
  labelCase: "none",
  deleteSupport: true,
  filterMetaData: {
    selectMode: "multiple",
    options: [
      { label: "Shallow", value: "shallow" },
      { label: "Good", value: "good" },
      { label: "Heavy", value: "heavy" }
    ],
    sortOptions: false
  },
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const PrCreatedInFilterConfig: LevelOpsFilter = {
  id: "pr_created_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Pr Created In",
  beKey: "pr_created_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const PrClosedTimeFilterConfig: LevelOpsFilter = {
  id: "pr_closed_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Pr Closed Time",
  beKey: "pr_closed_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const CommittedInFilterConfig: LevelOpsFilter = {
  id: "committed_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Committed In",
  beKey: "committed_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const CodeChangeSizeFilterConfig: LevelOpsFilter = {
  id: "code_change_sizes",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Code Change Size",
  beKey: "code_change_sizes",
  labelCase: "none",
  deleteSupport: true,
  filterMetaData: {
    selectMode: "multiple",
    options: [
      { label: "Small", value: "small" },
      { label: "Medium", value: "medium" },
      { label: "Large", value: "large" }
    ],
    sortOptions: false
  },
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const TimeFilterConfig: LevelOpsFilter = {
  id: "time_range",
  renderComponent: UniversalTimeBasedFilter,
  label: "Time",
  beKey: "time_range",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const PrMergedAtFilterConfig: LevelOpsFilter = {
  id: "pr_merged_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Pr Merged In",
  beKey: "pr_merged_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const FileTypeFilterConfig: LevelOpsFilter = {
  id: "file_types",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  beKey: "file_types",
  label: "File Type",
  labelCase: "title_case",
  deleteSupport: true,
  excludeSupport: true,
  partialKey: "file_type",
  supportPaginatedSelect: true,
  filterMetaData: {
    selectMode: "multiple",
    uri: "github_commits_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["file_type"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((Data: any) => Object.keys(Data)[0] === "file_type");
      if (currData) {
        return (Object.values(currData)[0] as Array<any>)
          ?.map((item: any) => ({
            label: item.key,
            value: item.key
          }))
          .filter((item: { label: string; value: string }) => !!item.value);
      }
      return [];
    },
    sortOptions: true,
    createOption: false
  } as ApiDropDownData,
  apiFilterProps: genericApiFilterProps,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const PrUpdatedAtFilterConfig: LevelOpsFilter = {
  id: "pr_updated_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Pr Updated In",
  beKey: "pr_updated_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
