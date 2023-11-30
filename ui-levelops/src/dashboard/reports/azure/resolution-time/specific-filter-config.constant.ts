import { generateMetricFilterConfig } from "dashboard/report-filters/common/metrics-filter.config";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS, METRIC_OPTIONS } from "./constant";
import { removeCustomPrefix } from "../common-helper";

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

export const MetricFilterConfig: LevelOpsFilter = generateMetricFilterConfig(
  METRIC_OPTIONS,
  "multiple",
  ["median_resolution_time", "number_of_tickets_closed"],
  "metric"
);
