import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

const ticketCategorizationUnitFilterOptions = [
  {
    label: "Ticket Count",
    value: "tickets_report"
  },
  { label: "Story Point", value: "story_point_report" },
  { label: "Commit count", value: "commit_count_fte" },
  { label: "Ticket Time Spent", value: "effort_investment_time_spent" }
];

export const EffortUnitFilterConfig: LevelOpsFilter = {
  id: "uri_unit",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Effort Unit",
  beKey: "uri_unit",
  labelCase: "title_case",
  defaultValue: "tickets_report",
  filterMetaData: {
    options: ticketCategorizationUnitFilterOptions,
    selectMode: "default",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateEffortUnitFilter = (metadata?: {
  effortUnitOptions: {
    label: string;
    value: string;
  }[];
  defaultValue: string;
}): LevelOpsFilter =>
  ({
    ...EffortUnitFilterConfig,
    defaultValue: metadata?.defaultValue ?? "tickets_report",
    filterMetaData: {
      ...(EffortUnitFilterConfig.filterMetaData ?? {}),
      options: metadata?.effortUnitOptions ?? ticketCategorizationUnitFilterOptions
    }
  } as LevelOpsFilter);
