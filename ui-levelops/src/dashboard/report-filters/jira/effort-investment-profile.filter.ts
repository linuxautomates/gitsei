import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalEffortInvestmentProfileFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalEffortInvestmentProfileFilter";
import { EffortInvestmentProfileFilterData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const EffortInvestmentProfileFilterConfig: LevelOpsFilter = {
  id: "effort_investment_profile",
  renderComponent: UniversalEffortInvestmentProfileFilter,
  label: "Effort Investment Profile",
  beKey: "ticket_categorization_scheme",
  labelCase: "none",
  filterMetaData: {
    categorySelectionMode: "multiple",
    showDefaultScheme: false,
    withProfileCategory: true,
    isCategoryRequired: false
  } as EffortInvestmentProfileFilterData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateEffortInvestmentProfileFilter = (metadata?: {
  showDefaultScheme?: boolean;
  withProfileCategory?: boolean;
  categorySelectionMode?: "default" | "multiple";
  isCategoryRequired?: boolean | ((args: any) => boolean);
}): LevelOpsFilter => ({
  ...EffortInvestmentProfileFilterConfig,
  filterMetaData: {
    categorySelectionMode: metadata?.categorySelectionMode ?? "default",
    withProfileCategory: metadata?.withProfileCategory ?? true,
    showDefaultScheme: metadata?.showDefaultScheme ?? true,
    isCategoryRequired: metadata?.isCategoryRequired ?? false
  } as EffortInvestmentProfileFilterData
});
