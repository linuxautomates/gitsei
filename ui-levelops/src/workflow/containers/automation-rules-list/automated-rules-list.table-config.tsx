import { getBaseUrl } from "constants/routePaths";
import React from "react";
import { actionsColumn, updatedAtColumn, nameColumn, tableCell } from "utils/tableUtils";

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}/propels/automation-rules/edit?rule`, "name", 150, "id") },
  { ...updatedAtColumn() },
  {
    title: "Application",
    key: "object_type",
    dataIndex: "object_type",
    filterable: false,
    sortable: false,
    width: 100,
    filterType: "apiMultiSelect",
    filterField: "object_types",
    uri: "objects",
    searchField: "name",
    render: (item: any, record: any, index: number) => {
      return <div style={{ textTransform: "capitalize" }}>{(item || "").replace(/_/g, " ").toLowerCase()}</div>;
    }
  },
  {
    title: "Owner",
    key: "owner",
    dataIndex: "owner",
    filterable: false,
    sortable: false,
    width: 120,
    // filterType: "select",
    // filterField: "enabled",
    // filterLabel: "Enabled",
    // options: [
    //   { label: "Enabled", value: true, key: true },
    //   { label: "Disabled", value: false, key: false }
    // ],
    render: (item: any, record: any, index: number) => tableCell(undefined, item)
  },
  { ...actionsColumn() }
];
