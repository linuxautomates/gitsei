import { genericDrilldownTransformer } from "./genericDrilldownTransformer";

export const microsoftDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  return { acrossValue, filters };
};
