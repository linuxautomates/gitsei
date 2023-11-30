import { baseColumnConfig } from "utils/base-table-config";
import { timeColumn } from "./common-table-columns";

export const rawStatsTableConfig = [
  baseColumnConfig("Issue", "key"),
  baseColumnConfig("Summary", "summary"),
  timeColumn("Issue Resolved Date", "issue_resolved_at")
];
