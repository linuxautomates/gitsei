import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { makeObjectKeyAsValue } from "utils/commonUtils";

export const pagerdutySupportedFilters: supportedFilterType = {
  uri: "pagerduty_filter_values",
  values: ["pd_service", "incident_priority", "incident_urgency", "alert_severity", "user_id"]
};

export const PAGERDUTY_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  pd_service: "pd_service_ids",
  user_id: "user_ids",
  incident_priority: "incident_priorities",
  incident_urgency: "incident_urgencies",
  alert_severity: "alert_severities"
};

export const PAGERDUTY_SERVICE_FILTER_KEY_MAPPING: Record<string, string> = {
  pd_service: "pd_service_id",
  cicd_job_id: "cicd_job_ids"
};

export const REVERSE_PAGERDUTY_COMMON_FILTER_KEY_MAPPING: Record<string, string> = makeObjectKeyAsValue(
  PAGERDUTY_COMMON_FILTER_KEY_MAPPING
);
