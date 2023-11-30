import { baseColumnConfig } from "utils/base-table-config";
import { updatedAtColumn } from "utils/tableUtils";

export const LevelopsIssuesTableConfig = [
  baseColumnConfig("Issue", "vanity_id"),
  baseColumnConfig("Title", "title", { width: "20%" }),
  { ...updatedAtColumn() }
];
