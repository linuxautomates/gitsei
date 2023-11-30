import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { removeCustomPrefix } from "../common-helper";
import { ACROSS_OPTIONS } from "./constant";

export const AcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      const commonOptions = ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({
        label: item.name,
        value: removeCustomPrefix(item)
      }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  getMappedValue: (data: any) => {
    const { allFilters } = data;
    if (allFilters?.across && allFilters?.interval) {
      return `${allFilters?.across}_${allFilters?.interval}`;
    }
    return undefined;
  },
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};
