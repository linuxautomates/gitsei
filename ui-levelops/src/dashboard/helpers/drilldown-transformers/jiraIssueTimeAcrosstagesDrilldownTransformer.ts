import { get } from "lodash";
import { jiraDrilldownTransformer } from ".";

export const jiraIssueTimeAcrossStagesDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = jiraDrilldownTransformer(data);
  const { drillDownProps } = data;
  if (acrossValue === "none") {
    const x_axis = get(drillDownProps, ["x_axis"], "");
    const filterField = "stages";
    delete filters?.filter?.none;
    filters = {
      ...filters,
      filter: {
        ...filters?.filter,
        [filterField]: [x_axis]
      }
    };
  }
  return { acrossValue, filters };
};
