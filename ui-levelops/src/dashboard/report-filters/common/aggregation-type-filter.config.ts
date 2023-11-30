import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const AggregationTypesFilterConfig: LevelOpsFilter = {
  id: "agg_type",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Aggregation Type",
  beKey: "agg_type",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    options: [
      {
        label: "Average",
        value: "average"
      },
      {
        label: "Total",
        value: "total"
      }
    ],
    selectMode: "default",
    sortOptions: true
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

export const generateAggregationTypesFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>)
): LevelOpsFilter => ({
  ...AggregationTypesFilterConfig,
  filterMetaData: { ...AggregationTypesFilterConfig.filterMetaData, options } as DropDownData
});
