import { capitalize } from "lodash";
import { tableCell } from "utils/tableUtils";

import { genericCsvTransformer } from "./genericCsvTransformer";

export const workItemCSVTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders } = data;
  const transformedData = apiData.map((record: any) => {
    return [...(jsxHeaders || []), ...columns]
      .map((col: any) => {
        switch (col.key) {
          case "vanity_id":
            return record?.["vanity_id"] || "";
          case "title": {
            let title = record["title"];
            if (title?.includes(",")) {
              title = `"${title}"`;
            }
            return title;
          }
          case "updated_at": {
            let value = record[col.key];
            return tableCell("created_at", value);
          }
          case "tag_ids": {
            let value = record[col.key];
            if (Array.isArray(value)) {
              if (!value?.length) {
                return "";
              }
              value = value?.join(",");
            }
            return `"${value}"`;
          }
          case "reporter":
            return record["reporter"];
          case "assignees": {
            let assignees = record["assignees"];
            if (!assignees?.length) return "";
            assignees = assignees?.map((item: any) => item?.user_email);
            return `"${assignees?.join(",")}"`;
          }
          case "status":
            return capitalize(record["status"]);
          case "priority":
            return capitalize(record["priority"]);
          default:
            return genericCsvTransformer(col?.key, record);
        }
      })
      .join(",");
  });
  return transformedData;
};
