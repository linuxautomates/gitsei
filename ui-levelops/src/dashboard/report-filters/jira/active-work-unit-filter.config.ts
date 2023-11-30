import React from "react";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import UniversalSelectFilterWrapper, {
  UniversalFilterWrapperProps
} from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

const jiraActiveWorkUnitOptions = [
  {
    label: "Ticket Count",
    value: "active_effort_investment_tickets"
  },
  { label: "Story Point", value: "active_effort_investment_story_points" }
];

export const filterRenderComponent: React.FC<UniversalFilterWrapperProps> = (props: any) => {
  if (!useHasEntitlements(Entitlement.BA_TREND_CURRENT_ALLOCATION_DISABLED, EntitlementCheckType.AND)) {
    return React.createElement(UniversalSelectFilterWrapper, { ...props });
  }
  return null;
};

export const ActiveWorkUnitFilterConfig: LevelOpsFilter = {
  id: "active_work_unit",
  renderComponent: filterRenderComponent,
  label: "Active Work Unit",
  beKey: "active_work_unit",
  labelCase: "title_case",
  defaultValue: "active_effort_investment_tickets",
  filterMetaData: {
    options: jiraActiveWorkUnitOptions,
    selectMode: "default",
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateActiveWorkUnitFilter = (metadata?: {
  activeWorkUnitOptions: {
    label: string;
    value: string;
  }[];
  defaultValue: string;
}): LevelOpsFilter =>
  ({
    ...ActiveWorkUnitFilterConfig,
    defaultValue: metadata?.defaultValue ?? "active_effort_investment_tickets",
    filterMetaData: {
      ...(ActiveWorkUnitFilterConfig.filterMetaData ?? {}),
      options: metadata?.activeWorkUnitOptions ?? jiraActiveWorkUnitOptions
    }
  } as LevelOpsFilter);
