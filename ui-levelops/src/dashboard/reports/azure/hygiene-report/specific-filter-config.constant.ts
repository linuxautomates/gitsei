import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputFilter";
import UniversalTextSwitchWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTextSwitchWrapper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const PoorDescriptionFilterConfig: LevelOpsFilter = {
  id: "poor_description",
  renderComponent: UniversalInputFilterWrapper,
  label: "Poor Description Length (Number Of Characters)",
  beKey: "poor_description",
  labelCase: "title_case",
  filterMetaData: { type: "number" },
  defaultValue: 10,
  deleteSupport: true,
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

export const IdleLengthFilterConfig: LevelOpsFilter = {
  id: "idle",
  renderComponent: UniversalInputFilterWrapper,
  label: "Idle Length (Days)",
  beKey: "idle",
  labelCase: "title_case",
  filterMetaData: {
    type: "number"
  },
  defaultValue: 30,
  deleteSupport: true,
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

export const HideScoreFilterConfig: LevelOpsFilter = {
  id: "hideScore",
  renderComponent: UniversalTextSwitchWrapper,
  label: "Hide Score",
  beKey: "hideScore",
  labelCase: "title_case",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
