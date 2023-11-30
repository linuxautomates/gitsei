import { ReactNode } from "react";
import { tableCell } from "utils/tableUtils";

export const getTableColumns = (renderJson: (value: string, name?: string) => ReactNode) => [
  {
    title: "operation",
    dataIndex: "operation",
    key: "operation",
    render: (value: any) => tableCell("operation", value),
    width: 100
  },
  {
    title: "path",
    dataIndex: "path",
    key: "path",
    width: 200
  },
  {
    title: "before",
    dataIndex: "before",
    key: "before",
    width: 300,
    render: (value: any) => renderJson(value, "before")
  },
  {
    title: "after",
    dataIndex: "after",
    key: "after",
    width: 300,
    render: (value: any) => renderJson(value, "after")
  }
];
