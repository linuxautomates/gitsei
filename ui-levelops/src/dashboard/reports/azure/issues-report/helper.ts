import { AZURE_TIME_FILTERS_KEYS, GROUP_BY_TIME_FILTERS, ID_FILTERS } from "constants/filters";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { capitalize, get } from "lodash";
import { TooltipPayload } from "recharts";
import { ChartType } from "../../../../shared-resources/containers/chart-container/ChartType";
import widgetConstants from "../../../constants/widgetConstants";
import {
  azureIssuesReportMetricsChartMapping,
  TOTAL_EFFORT,
  TOTAL_NUMBER_OF_TICKETS,
  TOTAL_SUM_OF_STORY_POINTS
} from "./constant";
import { AzureIssuesReportMetricTypes } from "./enums";

export const getTotalLabel = (data: { unit: string }) => {
  const { unit } = data;
  switch (unit) {
    case "Story Points":
      return TOTAL_SUM_OF_STORY_POINTS;
    case "Effort":
      return TOTAL_EFFORT;
    default:
      return TOTAL_NUMBER_OF_TICKETS;
  }
};

export const azureIssuesChartTooltipTransformer = (
  payload: { [x: string]: any },
  currentLabel: string,
  extra?: { chartType: ChartType; chartProps?: any }
) => {
  let totalCount = 0;
  const items = (payload || []).map((item: TooltipPayload, i: number) => {
    const label = azureIssuesReportMetricsChartMapping[item.name as AzureIssuesReportMetricTypes];
    totalCount += +item.value;
    return {
      label: label ?? item?.name,
      value: item.value,
      color: item.fill
    };
  });

  const hasTotalStoryPoints = payload.find(
    (_p: { name: string }) => _p.name === AzureIssuesReportMetricTypes.TOTAL_STORY_POINTS
  );
  const hasTotalTickets = payload.find(
    (_p: { name: string }) => _p.name === AzureIssuesReportMetricTypes.TOTAL_TICKETS
  );
  const hasEffort = payload.find((_p: { name: string }) => _p.name === AzureIssuesReportMetricTypes.TOTAL_EFFORT);

  const getTotalLabel = get(widgetConstants, [extra?.chartProps?.reportType, "getTotalLabel"], undefined);
  if (getTotalLabel) {
    const totalLabel = getTotalLabel(extra?.chartProps);
    if (
      !(hasTotalStoryPoints && totalLabel === TOTAL_SUM_OF_STORY_POINTS) &&
      !(hasTotalTickets && totalLabel === TOTAL_NUMBER_OF_TICKETS) &&
      !(hasEffort && totalLabel === TOTAL_EFFORT)
    ) {
      items.push({ label: totalLabel, value: totalCount, color: "#000" });
    }
  }
  return {
    list: items
  };
};

export const getAcrossValue = (args: any) => {
  const filters = args?.allFilters;
  let across = filters?.across;
  if ([...GROUP_BY_TIME_FILTERS, "ticket_created", ...AZURE_TIME_FILTERS_KEYS].includes(across)) {
    const interval = get(filters, ["interval"]);
    if (interval) across = `${across}_${interval}`;
  }
  return across;
};

export const onChartClickPayload = (params: { [key: string]: any }) => {
  const { data, across, visualization } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;
  if (visualization && visualization === ChartType.DONUT) {
    keyValue = get(data, ["tooltipPayload", 0, "name"], "_UNASSIGNED_");

    if (ID_FILTERS.includes(across)) {
      keyValue = {
        id: get(data, ["key"], "_UNASSIGNED_"),
        name: get(data, ["name"], "_UNASSIGNED_")
      };
    }

    if (["teams", "code_area"].includes(across)) {
      const _data = get(data, ["tooltipPayload", 0, "payload", "payload"]);
      keyValue = { ..._data, id: _data.key };
    }

    return keyValue;
  } else {
    if (ID_FILTERS.includes(across)) {
      keyValue = {
        id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
        name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
      };
    }

    if (["teams", "code_area"].includes(across)) {
      const _data = data?.activePayload?.[0]?.payload || {};
      keyValue = _data;
    }
  }
  return keyValue;
};

export const shouldReverseAPIData = (params: any) => {
  const { interval, across } = params;
  let should = false;
  if (["workitem_created_at", "workitem_resolved_at", "workitem_updated_at", "trend"].includes(across)) {
    should = true;
  }

  return should;
};

export const stackFilterStatus = (filters: any) => {
  return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
};

export const sortAPIDataHandler = (params: any) => {
  const { across, apiData = [] } = params;
  if (["workitem_created_at", "workitem_resolved_at", "workitem_updated_at", "trend"].includes(across)) {
    return apiData.sort((a: any, b: any) => b.key - a.key);
  }
  return apiData;
};

export const getDrillDownType = (params: any) => {
  const across = get(params, "across", undefined);
  if (across && across === "parent_workitem_id") {
    return "Feature";
  }
  if(across && across?.includes("Custom.")){
    return across.split(".").pop();
  }
  return across && capitalize(across.replace(/_/g, " "));
};
