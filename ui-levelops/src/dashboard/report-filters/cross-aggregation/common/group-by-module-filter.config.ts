import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import GroupByModuleFilterComponent from "dashboard/graph-filters/components/GenericFilterComponents/GroupByModuleFilterComponent";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { forEach, get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const CrossAggregationGrpByModuleFilterConfig: LevelOpsFilter = {
  id: "group-by-modules",
  renderComponent: GroupByModuleFilterComponent,
  label: "Group By Modules",
  defaultValue: true,
  updateInWidgetMetadata: true,
  beKey: "groupByRootFolder",
  filterInfo: "Root folders are the top most folders in a file system",
  labelCase: "title_case",
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  isSelected: (args: any) => {
    const { metadata } = args;
    let exist = false;
    forEach(Object.keys(metadata), (key: string) => {
      if (key.includes("groupByRootFolder") && get(metadata, [key])) {
        exist = true;
      }
    });
    return exist;
  },
  filterMetaData: {
    checkboxLabel: "Group By Modules"
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateGrpByModuleFilterConfig = (beKey: string) => ({
  ...CrossAggregationGrpByModuleFilterConfig,
  beKey: beKey
});
