import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import { EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS, EXCLUDE_SETTINGS_DEFAULT_OPTIONS } from "../constant";

export const buildExcludeObject = (
  profile: RestTrellisScoreProfile,
  event: any,
  key: string,
  type: string,
  isNumeric: boolean = false
) => {
  const { settings } = profile;
  let obj: any = {};
  const inputObj = (isNumeric ? settings?.exclude?.[key] : settings?.exclude?.partial_match?.[key]) || {};
  const defaultOption = isNumeric ? EXCLUDE_NUMBER_SETTINGS_DEFAULT_OPTIONS : EXCLUDE_SETTINGS_DEFAULT_OPTIONS;
  if (type === "dropdown") {
    obj[event] = Object.values(inputObj)?.[0] || "";
  }
  if (type === "input") {
    const inputKey = Object.keys(inputObj)?.[0] || defaultOption;
    obj[inputKey] = event.target.value;
  }

  return obj;
};

export const validateProfile = (trellisProfile: any) => {
  switch (trellisProfile !== undefined) {
    case !trellisProfile.json.name:
      return "Please enter profile name";
    case !trellisProfile.validate:
      return "Please select effort investment categories for features in Impact Tab";
    case trellisProfile.json?.sections.length > 0:
      return trellisProfile.json.sections.reduce(
        (acc: any, section: Record<string, any>, i: number, arr: Array<any>) => {
          if (section.enabled) {
            acc = section?.features?.find(
              (subSection: any) => subSection?.enabled && !subSection?.hasOwnProperty("max_value")
            );
          }
          if (acc) {
            arr.splice(1); // break reduce
            return `Please enter max value in ${acc?.name}`;
          }
        },
        false
      );
    default:
      return false;
  }
};
