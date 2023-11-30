import { updatedAtColumn, actionsColumn } from "utils/tableUtils";
import React from "react";
import { Link } from "react-router-dom";
import { AntText } from "shared-resources/components";
import { getSettingsPage } from "constants/routePaths";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

const NameColumn = ({ record }) => {
  const access = useConfigScreenPermissions();
  const name = `${record.first_name} ${record.last_name}`;
  return (
    <AntText className={"pl-10 ellipsis"}>
      {access[1] ? <Link to={`${getSettingsPage()}/edit-user-page?user=${record.id}`}>{name}</Link> : name}
    </AntText>
  );
};

export const tableColumns = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "first_name",
    dataIndex: "first_name",
    width: 300,
    ellipsis: true,
    render: (item, record, index) => {
      return <NameColumn record={record} />;
    }
  },
  { ...updatedAtColumn() },
  {
    title: "Email",
    dataIndex: "email",
    key: "email",
    width: 200
  },
  {
    title: "Type",
    dataIndex: "user_type",
    key: "user_type",
    width: 100
  },
  {
    title: "MFA Status",
    dataIndex: "mfa_enabled",
    key: "mfa_enabled",
    render: item => {
      return <AntText className={"pl-10"}>{!item ? "Disabled" : "Active"}</AntText>;
    },
    width: 100
  },
  {
    title: "Trellis Access",
    dataIndex: "scopes",
    key: "scopes",
    width: 100
  },
  { ...actionsColumn() }
];
