import { tableCell, actionsColumn } from "utils/tableUtils";

export const tableColumns = [
  {
    title: "Name",
    dataIndex: "name",
    key: "name",
    ellipsis: true,
    width: 250
  },
  {
    title: "Created On",
    dataIndex: "created_at",
    key: "created_at",
    width: 100,
    render: (props: any) => tableCell("created_at", props)
  },
  {
    title: "Description",
    dataIndex: "description",
    key: "description",
    width: 200,
    ellipsis: true
  },
  {
    title: "Role",
    dataIndex: "role",
    key: "role",
    width: 100
  },
  { ...actionsColumn() }
];
