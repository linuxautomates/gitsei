import { zendeskSalesForceHygieneTypes } from "dashboard/constants/hygiene.constants";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { salesforceCommonFiltersConfig } from "dashboard/report-filters/salesforce/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { SalesforceAcrossOptions } from "../constants";

export const SalesforceBounceReportFiltersConfig: LevelOpsFilter[] = [
  ...salesforceCommonFiltersConfig,
  generateHygieneFilterConfig(
    zendeskSalesForceHygieneTypes.map((item: string) => ({ label: item, value: item })),
    undefined,
    undefined,
    false
  ),
  generateAcrossFilterConfig(SalesforceAcrossOptions),
  MaxRecordsFilterConfig,
  supportManagementSystemFilterConfig
];
