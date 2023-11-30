import { get } from "lodash";
import { jiraDrilldownTransformer } from ".";

export const azureIssueTimeAcrossStagesDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = jiraDrilldownTransformer(data);
  const { drillDownProps } = data;
  if (acrossValue === "none") {
    const x_axis = get(drillDownProps, ["x_axis"], "");
    const filterField = "workitem_stages";
    delete filters?.filter?.none;
    filters = {
      ...filters,
      filter: {
        ...filters?.filter,
        [filterField]: [x_axis]
      }
    };
  }

  if (acrossValue === "code_area") {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        code_area: [drillDownProps?.["x_axis"]]
      }
    };
  }

  return { acrossValue, filters };
};
