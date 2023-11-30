import { timeColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";
import { baseColumnConfig } from "utils/base-table-config";
const ACTION_OPTIONS: string[] = [
  "DELETED",
  "SUCCESS",
  "EDITED",
  "PASSWORD_RESET_STARTED",
  "FAIL",
  "SUBMITTED",
  "ANSWERED",
  "PASSWORD_RESET_FINISHED",
  "SENT",
  "CREATED"
];
export const tableColumns = [
  {
    ...baseColumnConfig("Email", "email", { width: "20%" }),
    filterType: "feSelect",
    filterField: "emails",
    filterLabel: "Email"
  },
  baseColumnConfig("Body", "body", { width: "20%" }),
  baseColumnConfig("Type", "target_item_type", { width: "20%" }),
  {
    ...baseColumnConfig("Action", "action", { width: "20%" }),
    filterType: "multiSelect",
    filterField: "actions",
    filterLabel: "Action",
    options: ACTION_OPTIONS
  },
  timeColumn("Created At", "created_at", { width: "20%" })
];
