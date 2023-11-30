import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { FEBasedSelectFilterConfig } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { TICKET_CATEGORIZATION_UNIT_FILTER_KEY } from "../bussiness-alignment-applications/constants";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";

export const effortInvestmentUnit: FEBasedSelectFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Effort Unit",
  BE_key: TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  defaultValue: "tickets_report",
  options: [
    {
      label: "Ticket Count",
      value: "tickets_report"
    },
    { label: "Story Point", value: "story_point_report" }
  ],
  select_mode: "default"
};

export const progressReportEIUnit: FEBasedSelectFilterConfig = {
  type: WidgetFilterType.DROPDOWN_BASED_FILTERS,
  label: "Effort Unit",
  BE_key: TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  configTab: WIDGET_CONFIGURATION_KEYS.SETTINGS,
  defaultValue: "story_point_report",
  options: [
    {
      label: "Ticket Count",
      value: "tickets_report"
    },
    { label: "Story Point", value: "story_point_report" }
  ],
  select_mode: "default"
};
