import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalRadioBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalRadioBasedFilter";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const PrFilterConfig: LevelOpsFilter = {
  id: "pr-filter",
  renderComponent: UniversalRadioBasedFilter,
  label: "Pr Filter",
  beKey: "missing_fields",
  labelCase: "title_case",
  deleteSupport: true,
  apiFilterProps: args => ({ withDelete: withDeleteAPIProps(args) }),
  getMappedValue: args => {
    const { allFilters } = args;
    return get(allFilters, ["missing_fields", "pr_merged"], false);
  },
  modifiedFilterValueChange: (args: any) => {
    const { value, onModifiedFilterValueChange } = args;
    const payload = {
      parentKey: "missing_fields",
      value: value,
      type: "pr_merged"
    };
    onModifiedFilterValueChange?.(payload);
  },
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "PR CLOSED", value: true },
      { label: "PR MERGED", value: false }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
