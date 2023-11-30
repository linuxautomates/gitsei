import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const EpicsFilterConfig: LevelOpsFilter = {
  id: "epics",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Epics",
  beKey: "epics",
  labelCase: "none",
  deleteSupport: true,
  filterMetaData: {
    options: [],
    selectMode: "multiple",
    sortOptions: false,
    createOption: true
  } as DropDownData,
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
