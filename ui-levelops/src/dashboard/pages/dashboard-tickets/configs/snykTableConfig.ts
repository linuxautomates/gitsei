import { dateRangeFilterColumn, statusColumn, timeRangeFilterColumn } from "./common-table-columns";
import { baseColumnConfig } from "../../../../utils/base-table-config";
import { tableCell } from "../../../../utils/tableUtils";

export const SnykTableConfig = [
  baseColumnConfig("Title", "title"),
  statusColumn("Severity", "severity", { width: "5%" }),
  baseColumnConfig("Project", "project_name"),
  baseColumnConfig("Type", "type", { width: "7%" }),
  {
    ...baseColumnConfig("Disclosure Time", "disclosure_time"),
    render: (value: any) => tableCell("updated_on", value)
  },
  {
    ...baseColumnConfig("Publication Time", "publication_time"),
    render: (value: any) => tableCell("updated_on", value)
  },
  {
    ...baseColumnConfig("Project", "project", { hidden: true }),
    filterField: "project",
    filterLabel: "Project"
  },
  timeRangeFilterColumn("Priority Score", "Priority Score", "score_range"),
  dateRangeFilterColumn("Disclosure Time", "disclosure_range"),
  dateRangeFilterColumn("Publication Time", "publication_range")
];
