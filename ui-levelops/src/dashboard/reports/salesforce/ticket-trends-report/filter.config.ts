import { zendeskSalesForceHygieneTypes } from "dashboard/constants/hygiene.constants";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { salesforceCommonFiltersConfig } from "dashboard/report-filters/salesforce/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const SalesforceTicketsTrendReportFiltersConfig: LevelOpsFilter[] = [
  ...salesforceCommonFiltersConfig,
  generateHygieneFilterConfig(
    zendeskSalesForceHygieneTypes.map((item: string) => ({ label: item, value: item })),
    undefined,
    undefined,
    false
  )
];
