import { Link } from "react-router-dom";
import { tableCell, actionsColumn, updatedAtColumn } from "../utils/tableUtils";
import { AntText } from "../shared-resources/components";
import React from "react";
import { CONFIG_TABLE_ROUTES, getBaseUrl } from "../constants/routePaths";

export const tableColumns = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "name",
    dataIndex: "name",
    width: "100",
    ellipsis: true,
    render: (item, record, index) => {
      const _url = `${getBaseUrl()}${CONFIG_TABLE_ROUTES.EDIT}?id=${record.id}`;
      return (
        <AntText className={"pl-10"}>
          <Link className={"ellipsis"} to={_url}>
            {item}
          </Link>
        </AntText>
      );
    }
  },
  {
    title: "version",
    dataIndex: "version",
    key: "version",
    width: "10%"
  },
  {
    title: "Total Rows",
    dataIndex: "total_rows",
    key: "total_rows",
    width: "10%"
  },
  {
    title: "Created At",
    dataIndex: "created_at",
    key: "created_at",
    width: "10%",
    render: value => tableCell("created_at", value)
  },
  { ...updatedAtColumn() },
  { ...actionsColumn() }
];
