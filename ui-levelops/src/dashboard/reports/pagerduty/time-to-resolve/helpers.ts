import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";

export const timeToResolveXAxisLabelTransform = (params: any) => getXAxisLabel(params);

export const timeToResolveOnChartClickPayload = (params: any) => {
  const { across, data } = params;
  const _data = data?.activePayload?.[0]?.payload || {};
  if (["user_id", "pd_service"].includes(across)) {
    return {
      name: data.activeLabel || "",
      id: _data.key || data.activeLabel
    };
  }

  return data.activeLabel || "";
};
