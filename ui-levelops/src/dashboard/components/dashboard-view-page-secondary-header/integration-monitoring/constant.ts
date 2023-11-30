import { ColumnProps } from "antd/lib/table";
import { baseColumnConfig } from "utils/base-table-config";
import { tableCell } from "utils/tableUtils";

/* Defining the columns for the table. */
export const INTEGRATION_MONITORING_COLUMNS: Array<ColumnProps<any>> = [
  baseColumnConfig("Name", "name", { width: 150 }),
  {
    ...baseColumnConfig("Status", "status", { width: 100 }),
    render: (item, record) => {
      if (!record.statusUpdated) {
        return "Loading...";
      }
      return tableCell("status", item);
    }
  },
  {
    ...baseColumnConfig("Last Updated", "last_ingested_at", { width: 100 }),
    render: (item, record) => {
      if (!record.last_ingested_at) {
        return "Loading...";
      }
      return item;
    }
  }
];

export const MONITORED_INTEGRATION_LIST_UUID = "MONITORED_INTEGRATION_LIST_UUID";

/* A mapping of the integration status to a number. */
export const INTEGRATION_STATUS_PRIORITY_MAPPING = {
  failed: 1,
  unknown: 2,
  warning: 3,
  healthy: 4
};

/* A list of the possible statuses that an integration can have. */
export const INTEGRATION_STATUSES = ["failed", "unknown", "warning", "healthy"];
