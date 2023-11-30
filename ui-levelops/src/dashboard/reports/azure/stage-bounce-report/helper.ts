import { AZURE_TIME_FILTERS_KEYS } from "constants/filters";
import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "utils/dateUtils";

export const isStackFilterDisabled = ({ filters }: any) =>
  ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "stage"].includes(filters?.across);

export const onChartClickPayload = (params: any) => {
  const { data, across, stage } = params;
  let value = data.activeLabel ?? data.name ?? "";
  if (AZURE_TIME_FILTERS_KEYS.includes(across)) {
    value = convertEpochToDate(data.key, DEFAULT_DATE_FORMAT, true);
  }

  if (["assignee", "reporter"].includes(across)) {
    value = {
      id: data.key,
      name: data?.name ?? data?.additional_key ?? data.key
    };
  }

  return {
    value,
    stage: stage ? [stage] : undefined
  };
};

export const widgetValidationFunction = (payload: any) => {
  const { query = {} } = payload;
  let isValid = query?.workitem_stages && (query?.workitem_stages || []).length;
  if (isValid) {
    return true;
  } else if (!isValid && (query?.exclude?.workitem_stages || []).length) {
    return true;
  }
  return false;
};

export const stackFilterStatus = (filters: any) => {
  return filters && filters.across && [...AZURE_TIME_FILTERS_KEYS, "stage"].includes(filters.across);
};

export const getTotalKey = (params: any) => {
  const { metric } = params;
  return metric || "mean";
};
