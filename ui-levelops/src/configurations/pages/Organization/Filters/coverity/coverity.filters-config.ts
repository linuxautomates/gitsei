import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { coverityCommonFiltersConfig } from "dashboard/reports/coverity/coverity-specific-filter-config.constant";
import OrganisationFilterSelect from "../organisationFilterSelect";

export const OUCoverityCommonFiltersConfig = coverityCommonFiltersConfig.map((filter: LevelOpsFilter) => ({
  ...filter,
  renderComponent: OrganisationFilterSelect
}));
