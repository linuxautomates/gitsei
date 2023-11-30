import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import StateTranstionFilterComponent from "dashboard/graph-filters/components/StateTransitionFilterComponent";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const StateTrantitionFilterConfig: LevelOpsFilter = {
  id: "state_transition",
  renderComponent: StateTranstionFilterComponent,
  label: "State Transition",
  beKey: "state_transition",
  labelCase: "title_case",
  deleteSupport: true,
  isSelected: (args: any) => {
    return !!get(args.filters, ["state_transition"], undefined);
  },
  filterMetaData: {
    uri: "jira_filter_values"
  } as ApiDropDownData,
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
