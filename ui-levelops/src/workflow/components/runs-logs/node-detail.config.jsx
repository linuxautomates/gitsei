import React from "react";
import { baseColumnConfig } from "utils/base-table-config";
import { timeColumn, statusColumn } from "dashboard/pages/dashboard-tickets/configs/common-table-columns";

export const getItem = item => {
  if (typeof item === "object" && !Array.isArray(item)) {
    return JSON.stringify(item);
  }

  if (Array.isArray(item)) {
    return item.join();
  }

  return item.toString();
};

export const nodeDetailTableConfig = [
  {
    title: "#",
    key: "row_no",
    dataIndex: "row_no",
    width: "1%",
    render: (item, record, index) => <strong>{index + 1}</strong>
  },
  {
    title: "Key",
    key: "key",
    dataIndex: "key",
    width: "10%"
  },
  {
    title: "Content Type",
    key: "content_type",
    dataIndex: "content_type",
    width: "10%"
  },
  {
    title: "Value Type",
    key: "value_type",
    dataIndex: "value_type",
    width: "5%"
  },
  {
    title: "Value",
    key: "value",
    dataIndex: "value",
    width: "20%",
    render: (item, record, index) => {
      return <span>{getItem(item)}</span>;
    }
  }
];

export const runsLogsTableConfig = [
  baseColumnConfig("Node", "node_name", { width: "35%" }),
  timeColumn("Updated On", "state_changed_at", { width: "15%" }),
  statusColumn("State", "state", { width: "45%" })
];
