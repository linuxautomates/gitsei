import { zendeskSalesForceHygieneTypes } from "dashboard/constants/hygiene.constants";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { zendeskCommonFiltersConfig } from "dashboard/report-filters/zendesk/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { AcrossFilterConfig } from "./specific-filters-config.constant";

export const ZendeskTicketsReportFiltersConfig: LevelOpsFilter[] = [
  ...zendeskCommonFiltersConfig,
  generateHygieneFilterConfig(
    zendeskSalesForceHygieneTypes.map((item: string) => ({ label: item, value: item })),
    undefined,
    undefined,
    false
  ),
  { ...IssueCreatedAtFilterConfig, beKey: "created_at", label: "Zendesk Ticket Created In" },
  AcrossFilterConfig,
  MaxRecordsFilterConfig,
  supportManagementSystemFilterConfig,
  ShowValueOnBarConfig
];
