import { sprintStatCalculateBasedOnMetric } from "custom-hooks/helpers/sprintStatReporthelper";
import { sprintReportDataKeyTypes } from "dashboard/graph-filters/components/sprintFilters.constant";
import { get } from "lodash";
import { tableCell } from "utils/tableUtils";

const sprintStatMetricDataIndexMapping = {
  done_to_commit: "avg_commit_to_done",
  sprint_creep: "avg_creep",
  creep_completion: "avg_creep_to_done",
  creep_done_to_commit: "avg_creep_done_to_commit",
  creep_miss_completion: "avg_creep_to_miss",
  commit_miss_completion: "avg_commit_to_miss",
  committed_keys: "commit_tickets",
  creep_keys: "creep_tickets",
  delivered_creep_keys: "creep_done_tickets",
  commit_delivered_keys: "commit_done_tickets"
};

export const sprintMetricStatCsvTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders } = data;
  return (apiData || []).map((record: any) => {
    return [...(jsxHeaders || []), ...(columns || [])]
      .map((col: any) => {
        const dataKey = get(sprintStatMetricDataIndexMapping, [col.key], col.key);
        let result = record[dataKey] || "";
        if (typeof result === "string" && dataKey === "sprint_goal") {
          result = result.replace(/,/g, "").replace(/\s+/g, " ").trim();
        }
        if (dataKey === "key") {
          return tableCell("updated_on", result);
        }
        if (![...Object.values(sprintReportDataKeyTypes), "additional_key", "sprint_goal"].includes(dataKey)) {
          let { num } = sprintStatCalculateBasedOnMetric(dataKey, 0, record);
          if (
            !(
              (dataKey || "").includes("points") ||
              (dataKey || "").includes("tickets") ||
              (dataKey || "").includes("average")
            )
          ) {
            num *= 100;
          }

          if ((dataKey || "").includes("average")) {
            return num.toFixed(2);
          }
          return Math.floor(num);
        }
        return result;
      })
      .join(",");
  });
};
