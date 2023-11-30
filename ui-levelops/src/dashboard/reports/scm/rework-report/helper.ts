export const scmReworkOnChartClickPayloadHelper = (params: any) => {
  const { data, across } = params;
  const _data = data?.activePayload?.[0]?.payload || {};
  if (across === "trend") {
    return data.activeLabel;
  } else if (["author", "committer"].includes(across)) {
    return {
      name: data.activeLabel || "",
      id: _data.key || data.activeLabel
    };
  }
  return data.activeLabel || "";
};
