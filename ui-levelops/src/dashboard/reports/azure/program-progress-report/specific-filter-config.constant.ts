import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import JiraBAWidgetAcrossFilters from "dashboard/graph-filters/components/GenericFilterComponents/JiraBAWidgetAcrossFilters";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, EffortInvestmentProfileFilterData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const EffortUnitFilterConfig: LevelOpsFilter = {
  id: "uri_unit",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Effort Unit",
  beKey: "uri_unit",
  labelCase: "title_case",
  defaultValue: "tickets_report",
  filterMetaData: {
    options: [
      {
        label: "Ticket Count",
        value: "tickets_report"
      },
      { label: "Story Point", value: "story_point_report" }
    ],
    selectMode: "default",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const EffortInvProfile: LevelOpsFilter = {
  id: "effort_investment_profile",
  renderComponent: JiraBAWidgetAcrossFilters,
  label: "Effort Investment Profile",
  beKey: "ticket_categorization_scheme",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    categorySelectionMode: "default",
    withProfileCategory: false,
    showDefaultScheme: true,
    isCategoryRequired: false
  } as EffortInvestmentProfileFilterData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
