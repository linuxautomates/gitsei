import { valueToUnixTime } from "utils/dateUtils";
import { makeCSVSafeString } from "utils/stringUtils";
import { timeBoundFilterKeys } from "../../graph-filters/components/DateOptionConstants";

export const genericCsvTransformer = (columnKey: string, record: any) => {
  let result = record[columnKey];

  if (timeBoundFilterKeys.includes(columnKey)) {
    const unixTime = valueToUnixTime(record[columnKey]);
    result = unixTime ? `${unixTime}` : "";
  } else if (typeof result === "string") {
    // Commas and quotes break things. Need to handle.
    result = makeCSVSafeString(result);
  }

  return result;
};
