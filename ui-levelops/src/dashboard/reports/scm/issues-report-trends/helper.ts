export const scmIssuesTrendReportChartClickPayload = (params: any) => {
  const { data, across } = params;
  const _data = data?.activePayload?.[0]?.payload || {};
  return {
    name: data.activeLabel || "",
    id: _data.key || data.activeLabel
  };
};
