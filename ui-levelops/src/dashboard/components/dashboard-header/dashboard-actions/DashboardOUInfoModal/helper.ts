import { get } from "lodash";

export const getPrefixLabel = (filterType: string) => {
  switch (filterType) {
    case "normal":
      return "Equals";
    case "exclude":
      return "Does Not Equal";
    case "STARTS":
      return "Starts with";
    default:
      return "Contains";
  }
};
const getValue = (value: any) => {
  if (Array.isArray(value)) {
    return value;
  }
  if (value.hasOwnProperty("$begins")) {
    return value["$begins"];
  }
  if (value.hasOwnProperty("$contains")) {
    return value["$contains"];
  }
};
const getFilterObject = (key: string, dynamicUsers: any, type: string) => {
  const label = key?.replace("custom_field_", "");
  const value = getValue(dynamicUsers?.[key] || []);
  return {
    label: label,
    key: key,
    value: value,
    type: type
  };
};
export const getDynamicUsers = (dynamicUsers: any) => {
  let allUsers: any = [];
  const normalKeys = Object.keys(dynamicUsers || {}).filter(
    (key: string) => !["exclude", "partial_match"].includes(key)
  );
  const excludeKeys = Object.keys(dynamicUsers?.["exclude"] || {});
  const partialKeys = Object.keys(dynamicUsers?.["partial_match"] || {});
  if (normalKeys.length) {
    const normalusers = (normalKeys || [])?.map((key: string) => {
      return getFilterObject(key, dynamicUsers, "normal");
    });
    allUsers = [...allUsers, ...(normalusers || [])];
  }
  if (excludeKeys.length) {
    const excludeUsers = (excludeKeys || [])?.map((key: string) => {
      return getFilterObject(key, dynamicUsers["exclude"], "exclude");
    });
    allUsers = [...allUsers, ...(excludeUsers || [])];
  }
  if (partialKeys.length) {
    const partialUsers = (partialKeys || [])?.map((key: string) => {
      const type = get(dynamicUsers, ["partial_match", key], {})?.hasOwnProperty("$begins") ? "STARTS" : "CONTAINS";
      return getFilterObject(key, dynamicUsers["partial_match"], type);
    });
    allUsers = [...allUsers, ...(partialUsers || [])];
  }
  return allUsers;
};
