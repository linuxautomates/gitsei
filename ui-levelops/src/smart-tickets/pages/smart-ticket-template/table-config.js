import { tableCell, nameColumn, updatedAtColumn, actionsColumn } from "utils/tableUtils";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";

export const templateTableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}${TEMPLATE_ROUTES.ISSUE_TEMPLATE.EDIT}?template`) },
  { ...updatedAtColumn() },
  {
    title: "Enabled",
    dataIndex: "enabled",
    key: "enabled",
    width: 100,
    render: (item, record, index) => tableCell("enabled", item)
  },
  {
    title: "Default",
    dataIndex: "default",
    key: "default",
    width: 100,
    render: (item, record, index) => tableCell("default", item)
  },
  { ...actionsColumn() }
];
