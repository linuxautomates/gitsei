import { get } from "lodash";

export const scmCodingDaysOnChartClickHelper = (params: any) => {
  const { data, across } = params;
  const _data = data?.activePayload?.[0]?.payload || {};
  if (["author", "committer"].includes(across)) {
    return {
      name: data.activeLabel || "",
      id: _data.key || data.activeLabel
    };
  }
  return data.activeLabel || "";
};

export const scmCodingDaysWidgetValidationFunction = (payload: any) => {
  const { query } = payload;
  const committed_at = get(query, ["committed_at"], undefined);
  return committed_at ? true : false;
};
