import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export const salesforceSupportedFilters: supportedFilterType = {
  uri: "salesforce_filter_values",
  values: ["status", "priority", "type", "contact", "account_name"]
};

export const SALESFORCE_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  status: "statuses",
  priority: "priorities",
  type: "types",
  contact: "contacts",
  account_name: "accounts"
};

export const BASE_SALESFORCE_CHART_PROPS = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};
