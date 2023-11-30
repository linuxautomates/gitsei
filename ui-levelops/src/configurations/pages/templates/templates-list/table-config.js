import { tableCell, nameColumn, updatedAtColumn, actionsColumn } from "../../../../utils/tableUtils";
import { capitalize } from "lodash";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}${TEMPLATE_ROUTES.COMMUNICATION_TEMPLATES.EDIT}?template`) },
  { ...updatedAtColumn("created_at", "Created On") },
  {
    title: "Notification Type",
    key: "type",
    dataIndex: "type"
  },
  {
    title: "Default",
    key: "default",
    dataIndex: "default",
    render: (item, record, index) => tableCell("default", item)
  },
  {
    title: "Template Type",
    key: "event_type",
    dataIndex: "event_type",
    render: (item, record, index) => (item && typeof item === "string" ? capitalize(item.replace(/_/g, " ")) : "")
  },
  { ...actionsColumn() }
];
