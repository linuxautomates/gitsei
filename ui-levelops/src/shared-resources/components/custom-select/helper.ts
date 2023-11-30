import { lowerCase, upperCase } from "lodash";
import { toTitleCase } from "utils/stringUtils";

export type StringCaseType = "lower_case" | "upper_case" | "title_case" | "none";

export const convertStringCase = (value: string, string_case: StringCaseType): string => {
  switch (string_case) {
    case "lower_case":
      return lowerCase(value);
    case "upper_case":
      return upperCase(value);
    case "title_case":
      return toTitleCase(value);
    default:
      return value;
  }
};
