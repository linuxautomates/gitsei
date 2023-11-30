import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { SORT_OPTIONS } from "./constants";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";

export const SortFilterConfig: LevelOpsFilter = {
  id: "sort",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sort",
  beKey: "sort",
  labelCase: "none",
  filterMetaData: {
    selectMode: "default",
    sortOptions: true,
    options: SORT_OPTIONS,
    mapFilterValueForBE: (value: string) => [{ id: value, order: "-1" }]
  } as DropDownData,
  getMappedValue: (args: any) => {
    const sortFilterValues: { id: string; order: string }[] = get(args?.allFilters, ["sort"], { id: "" });
    return sortFilterValues?.length ? sortFilterValues[0].id : "";
  },
  deleteSupport: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
