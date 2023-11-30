import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { removeCustomPrefix } from "../common-helper";
import { STACK_OPTIONS } from "./constant";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: (args: any) => {
      const commonOptions = STACK_OPTIONS;
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
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    options: [
      { value: "week", label: "Weekly on Monday" },
      { value: "month", label: "First day of the month" },
      { value: "quarter", label: "First day of the quarter" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const LeftYAxisFilterConfig: LevelOpsFilter = {
  id: "leftYAxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Left Y-Axis",
  beKey: "leftYAxis",
  labelCase: "title_case",
  defaultValue: "total_tickets",
  updateInWidgetMetadata: true,
  filterMetaData: {
    options: [
      { value: "total_tickets", label: "Number of tickets" },
      { value: "total_story_points", label: "Sum of Story Points" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const RightYAxisFilterConfig: LevelOpsFilter = {
  id: "rightYAxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "rightYAxis",
  beKey: "rightYAxis",
  labelCase: "title_case",
  updateInWidgetMetadata: true,
  defaultValue: "median",
  filterMetaData: {
    options: [
      { value: "median", label: "Median Age of Tickets" },
      { value: "p90", label: "90th percentile Age of Tickets" },
      { value: "mean", label: "Average Age of Tickets" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
