import { baseColumnConfig } from "utils/base-table-config";
import {
  priorityColumn,
  userColumn,
  statusColumn,
  coloredTagsColumn,
  cautiousUnixTimeColumn
} from "dashboard/pages/dashboard-tickets/configs/common-table-columns";

export const MicrosoftIssuesTableConfig = [
  baseColumnConfig("Issue Type", "name"),
  baseColumnConfig("Category", "category"),
  priorityColumn(),
  userColumn("Owner", "owner"),
  baseColumnConfig("Description", "description"),
  baseColumnConfig("Model", "model"),
  statusColumn("State", "state"),
  coloredTagsColumn("Tags", "tags"),
  coloredTagsColumn("Projects", "projects"),
  cautiousUnixTimeColumn(),
  cautiousUnixTimeColumn("Ingested At", "ingested_at")
];
