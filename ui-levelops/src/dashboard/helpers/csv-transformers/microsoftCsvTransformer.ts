import { genericCsvTransformer } from "./genericCsvTransformer";
import { makeCSVSafeString } from "utils/stringUtils";

export const microsoftCsvTransformer = (columnKey: string, record: any) => {
  let value = record[columnKey];

  if (Array.isArray(value) && (columnKey === "tags" || columnKey === "projects")) {
    value = value.map((tag: any) => {
      const item_value = Object.values(tag || {})[0];
      if (item_value) return item_value;
      return "";
    });
    value = value.join(",");
    value = makeCSVSafeString(value);
  } else {
    value = genericCsvTransformer(columnKey, record);
  }

  return value;
};
