export const scmIssuesFirstResponseReportOnChartClickHelper = (params: any) => {
  const { data, across } = params;
  const payload = data?.activePayload?.[0]?.payload || {};
  if (["creator"].includes(across)) {
    return {
      name: data.activeLabel || "",
      id: payload.key || data.activeLabel
    };
  }
  if (["label"].includes(across)) {
    return payload.key || data.activeLabel;
  }
  return data.activeLabel || "";
};
