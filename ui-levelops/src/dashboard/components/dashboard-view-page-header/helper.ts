import { capitalize } from "lodash";
import { AddDashboardActionMenuType } from "./constant";

export const secondaryFilterExist = (metaData: any) => {
  if (metaData?.effort_investment_profile || metaData?.effort_investment_unit) {
    return true;
  }
  return false;
};

export const getAddDashboardActionMenuLabel = (key: AddDashboardActionMenuType) => {
  const splitKeys = key.split("_");
  return `${capitalize(splitKeys[0])} ${capitalize(splitKeys[1])}`;
};
