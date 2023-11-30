import { convertToDays } from "utils/timeUtils";
import { CSVTransformerParamType } from "../csvTranformer.type";
import { genericCsvTransformer } from "../genericCsvTransformer";

export const levelopsAssessmentTimeTableReportCSVTransformer = (data: CSVTransformerParamType) => {
  const { apiData, columns, jsxHeaders } = data;
  return apiData.map((record: any) => {
    return [...(jsxHeaders || []), ...(columns || [])]
      .map((col: any) => {
        switch (col.key) {
          case "min":
          case "max":
          case "median":
            return record[col.key] ? `${convertToDays(record[col.key])} days` : "";
          default:
            return genericCsvTransformer(col.key, record);
        }
      })
      .join(",");
  });
};
