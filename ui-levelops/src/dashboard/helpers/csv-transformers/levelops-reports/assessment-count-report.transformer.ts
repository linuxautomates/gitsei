import { getQnAProgress } from "assessments/utils/helper";
import { capitalize, get } from "lodash";
import { tableCell } from "utils/tableUtils";
import { CSVTransformerParamType } from "../csvTranformer.type";
import { genericCsvTransformer } from "../genericCsvTransformer";

export const levelopsAssessmentCountReportCSVTransformer = (data: CSVTransformerParamType) => {
  const { apiData, columns, jsxHeaders } = data;
  return apiData.map((record: any) => {
    return [...(jsxHeaders || []), ...(columns || [])]
      .map((col: any) => {
        switch (col.key) {
          case "created_at":
          case "updated_at": {
            let value = record[col.key];
            return tableCell("created_at", value);
          }
          case "progress":
            return `${getQnAProgress(record)} %`;
          case "tag_ids": {
            let value = record[col.key];
            if (Array.isArray(value)) {
              if (!value.length) {
                return "";
              }
              value = value.join(", ");
            }
            return `"${value}"`;
          }
          case "assignees": {
            const assignees = get(record, ["assignees"], []);
            if (!assignees.length) return "";
            return `"${assignees.join(",")}"`;
          }
          case "status":
            return capitalize((record["status"] || "").replace("_", " "));
          case "vanity_id":
            return record?.["vanity_id"] || "";
          case "priority":
            return capitalize(record["priority"]);
          default:
            return genericCsvTransformer(col.key, record);
        }
      })
      .join(",");
  });
};
