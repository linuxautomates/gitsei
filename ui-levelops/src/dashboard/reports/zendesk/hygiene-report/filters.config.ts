import { zendeskSalesForceHygieneTypes } from "dashboard/constants/hygiene.constants";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { zendeskCommonFiltersConfig } from "dashboard/report-filters/zendesk/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { HideScoreFilterConfig, HygieneWeightsFiltersConfig } from "./specific-filters-config.constant";

export const ZendeskHygieneReportFiltersConfig: LevelOpsFilter[] = [
  ...zendeskCommonFiltersConfig,
  { ...IssueCreatedAtFilterConfig, beKey: "created_at", label: "Zendesk Ticket Created In" },
  generateHygieneFilterConfig(
    zendeskSalesForceHygieneTypes.map((item: string) => ({ label: item, value: item })),
    undefined,
    undefined,
    false
  ),
  HygieneWeightsFiltersConfig,
  HideScoreFilterConfig
];
