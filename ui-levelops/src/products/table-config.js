import { actionsColumn, updatedAtColumn, nameColumn } from "../utils/tableUtils";

export const tableColumns = [
  { ...nameColumn() },
  { ...updatedAtColumn() },
  { ...updatedAtColumn("created_at", "Created At") },
  {
    title: "Owner",
    dataIndex: "owner",
    key: "owner",
    width: 200
  },
  { ...actionsColumn() }
];
