import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export const zendeskSupportedFilters: supportedFilterType = {
  uri: "zendesk_filter_values",
  values: ["brand", "type", "priority", "status", "organization", "assignee", "requester", "submitter"]
};

export const ZENDESK_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  status: "statuses",
  priority: "priorities",
  type: "types",
  organization: "organizations",
  assignee: "assignees",
  submitter: "submitters",
  brand: "brands",
  requester: "requesters"
};

export const BASE_ZENDESK_CHART_PROPS = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};
