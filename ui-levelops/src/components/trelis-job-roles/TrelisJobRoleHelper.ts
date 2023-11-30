import { TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { cloneDeep } from "lodash";
import { FEATURES_WITH_EFFORT_PROFILE } from "./constant";

export const metricKeyValueUpdate = (
  currentSubProfile: Record<any, any>,
  metric: string | Record<string, any>,
  key: string,
  value: any
) => {
  const newSubProfile = { ...currentSubProfile };
  newSubProfile?.sections?.forEach((section: any) => {
    section?.features?.forEach((feature: any) => {
      if (feature?.name === metric) {
        feature[key] = value;
      }
    });
  });
  return newSubProfile;
};

export const metricObjectUpdate = (currentSubProfile: Record<any, any>, metric: any) => {
  const newSubProfile = { ...currentSubProfile };
  newSubProfile?.sections?.forEach((section: any) => {
    section?.features?.forEach((feature: any, index: number) => {
      if (feature?.name === metric?.name) {
        section.features[index] = metric;
      }
    });
  });
  return newSubProfile;
};

export const validateTrellisProfile = (
  trellisProfile: any,
  orgUnit: any,
  parentNodeRequired: boolean,
  permission: boolean,
  trellisProfileIsEnabled: boolean,
  validName: boolean
) => {
  switch (true) {
    case !orgUnit:
      return "Please enter org name";
    case !validName:
      return "This collection name already exist";
    case parentNodeRequired:
      return "Please select parent Collection";
    case permission:
      return TOOLTIP_ACTION_NOT_ALLOWED;
    case trellisProfileIsEnabled && trellisProfile?.effort_investment_profile_id !== undefined: {
      let flag: string | boolean = false;
      if (!trellisProfile?.feature_ticket_categories_map[FEATURES_WITH_EFFORT_PROFILE[0]]) {
        flag = `Please select categories from advanced configuration for High Impact bugs worked on per month`;
      }
      if (!trellisProfile?.feature_ticket_categories_map[FEATURES_WITH_EFFORT_PROFILE[1]]) {
        flag = `Please select categories from advanced configuration for High Impact stories worked on per month`;
      }
      return flag;
    }

    default:
      return false;
  }
};
