import { bullseyeCommonFiltersConfig } from "dashboard/reports/bullseye/bullseye-specific-filter-config.constant";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";

export const OUBullseyeCommonFiltersConfig = bullseyeCommonFiltersConfig.map((filter: LevelOpsFilter) => ({
  ...filter,
  renderComponent: OrganisationFilterSelect
}));
