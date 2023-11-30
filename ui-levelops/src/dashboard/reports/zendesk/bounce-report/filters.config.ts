import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { zendeskSalesForceHygieneTypes } from "dashboard/constants/hygiene.constants";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { generateHygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { supportManagementSystemFilterConfig } from "dashboard/report-filters/cross-aggregation/common/supportManagementFilters.config";
import { zendeskCommonFiltersConfig } from "dashboard/report-filters/zendesk/common-filters.config";
import { get, uniqBy } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "../commonZendeskReports.constants";

export const ZendeskBounceReportFiltersConfig: LevelOpsFilter[] = [
  ...zendeskCommonFiltersConfig,
  generateHygieneFilterConfig(
    zendeskSalesForceHygieneTypes.map((item: string) => ({ label: item, value: item })),
    undefined,
    undefined,
    false
  ),
  { ...IssueCreatedAtFilterConfig, beKey: "created_at", label: "Zendesk Ticket Created In" },
  generateAcrossFilterConfig((args: any) => {
    const commonOptions = ACROSS_OPTIONS;
    const filterMetaData = get(args, ["filterMetaData"], {});
    const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
    const customFieldsOptions = customFieldsData.map((item: any) => ({
      label: item.name,
      value: `${CUSTOM_FIELD_PREFIX}${item.key}`
    }));
    return uniqBy([...commonOptions, ...customFieldsOptions], "value");
  }),
  MaxRecordsFilterConfig,
  supportManagementSystemFilterConfig
];
