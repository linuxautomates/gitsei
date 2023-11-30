import { convertEpochToDate, DEFAULT_DATE_FORMAT } from "../../../../utils/dateUtils";
import { GROUP_BY_TIME_FILTERS } from "../../../../constants/filters";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";

export const stageBounceValidationFunc = (payload: any) => {
  const { query = {} } = payload;
  let isValid = query?.stages && (query?.stages || []).length;
  if (isValid) {
    return true;
  } else if (!isValid && (query?.exclude?.stages || []).length) {
    return true;
  }
  return false;
};

export const getStageBounceTotalKey = (params: any) => {
  const { metric } = params;
  return metric || "mean";
};

export const getStackStatus = (filters: any) => {
  return filters && filters.across && filters.across === "stage";
};

export const stageBounceXAxisLabelTransform = (params: any) => getXAxisLabel(params);

export const stageBounceChartClickPayload = (params: any) => {
  const { data, across, stage } = params;
  let value = data.activeLabel ?? data.name ?? "";
  if (GROUP_BY_TIME_FILTERS.includes(across)) {
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

export const isStackFilterDisabled = ({ filters }: any) =>
  ["issue_created", "issue_updated", "issue_resolved", "stage"].includes(filters?.across);
