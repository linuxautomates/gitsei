import { updatedAtColumn } from "utils/tableUtils";
import React from "react";
import { AntText } from "../shared-resources/components";
import { Tag } from "antd";
import { RBAC } from "constants/localStorageKeys";
import "./dashboard-list.styles.scss";
import { getHomePage } from "constants/routePaths";
import { get } from "lodash";
import { useParams, Link } from "react-router-dom";

const NameRenderer = props => {
  const params = useParams();
  const _url = `${getHomePage(params)}?dashboard_id=${props.id}`;
  return (
    <AntText className={"pl-10"}>
      <Link className={props.classname} to={_url}>
        {props.displayName}
      </Link>
    </AntText>
  );
};

export const tableColumns = [
  {
    title: <span className={"pl-10"}>Name</span>,
    key: "name",
    dataIndex: "name",
    width: 300,
    ellipsis: true,
    sorter: true,
    render: (item, record, index) => {
      const userRole = localStorage.getItem(RBAC);
      let classname = "ellipsis";
      const displayName = get(record, ["metadata", "display_name"], item);
      if (userRole === "PUBLIC_DASHBOARD" && !record.public) {
        classname = "nonPublicDashboard";
      }
      return <NameRenderer className={classname} displayName={displayName} id={record.id} />;
    }
  },
  {
    ...updatedAtColumn("updated_at"),
    width: 200,
    sorter: true
  },
  {
    title: "Created By",
    dataIndex: "email",
    key: "email",
    width: 200,
    sorter: true,
    render: (item, record, index) => {
      const rbac = localStorage.getItem(RBAC);
      if (rbac === "PUBLIC_DASHBOARD") {
        return (
          <AntText className={"pl-10"}>{`${record.owner_first_name || ""} ${record.owner_last_name || ""}`}</AntText>
        );
      } else {
        return <AntText className={"pl-10"}>{`${record.owner || ""}`}</AntText>;
      }
    }
  },
  {
    title: "Default",
    dataIndex: "default",
    key: "default",
    render: (item, record, index) => {
      if (item === true) {
        return <Tag color={"green"}>DEFAULT</Tag>;
      }
    }
  }
];
