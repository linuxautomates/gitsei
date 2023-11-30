import { tableCell, updatedAtColumn, nameColumn } from "utils/tableUtils";
import React from "react";
import { getWorkitemDetailPage } from "constants/routePaths";
import { NameAvatar } from "shared-resources/components";
import { AvatarWithText } from "../../shared-resources/components";
import { Row, Col } from "antd";
import { WORKSPACES } from "dashboard/constants/applications/names";

export const tableColumns = () => [
  {
    ...nameColumn(`${getWorkitemDetailPage()}?workitem`, "vanity_id", 150, "vanity_id")
  },
  {
    title: "Title",
    dataIndex: "title",
    key: "title",
    width: "20%",
    ellipsis: true,
    colSpan: 2,
    align: "left"
  },
  {
    title: "Project",
    dataIndex: "products",
    key: "workspace",
    //width: "15%",
    filterType: "apiMultiSelect",
    filterField: "product_ids",
    uri: "workspace",
    filterLabel: WORKSPACES,
    ellipsis: true,
    colSpan: 0,
    width: 0,
    render: (item, record, index) => null,
    span: 8
  },
  {
    ...updatedAtColumn(),
    filterType: "dateRange",
    filterLabel: "Updated Between",
    filterField: "updated_at",
    convertToNumber: true,
    span: 8
  },
  {
    title: "Reporter",
    dataIndex: "reporter",
    key: "reporter",
    width: 250,
    filterType: "apiSelect",
    filterField: "reporter",
    uri: "users",
    searchField: "email",
    specialKey: "email",
    ellipsis: true,
    render: (item, record, index) => <AvatarWithText text={item} />,
    span: 8
  },
  {
    title: "Assignees",
    dataIndex: "assignees",
    key: "assignees",
    ellipsis: true,
    render: (item, record, index) => {
      if (Array.isArray(item)) {
        if (!!item.length) {
          return (
            <Row justify={"start"} type={"flex"} gutter={[0, 0]}>
              {item.map(user => (
                <Col span={4} style={{ margin: "2px 0" }}>
                  <NameAvatar name={user.user_email} />
                </Col>
              ))}
            </Row>
          );
        } else {
          return "UNASSIGNED";
        }
      }
      return "UNASSIGNED";
    },
    width: 150,
    filterType: "apiMultiSelect",
    filterField: "assignee_user_ids",
    uri: "users",
    searchField: "email",
    span: 8
    //specialKey: "email",
    //options: [{ id: "UNASSIGNED", email: "UNASSIGNED" }]
  },
  {
    title: "Tags",
    filterTitle: "Tags",
    key: "tag_ids",
    dataIndex: "tag_ids",
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    uri: "tags",
    width: 200,
    ellipsis: true,
    render: value => tableCell("tags", value),
    span: 8
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    render: props => tableCell("status", props),
    width: 120,
    filterType: "apiSelect",
    filterField: "status",
    searchField: "name",
    specialKey: "name",
    uri: "states",
    span: 8
    //options: RestWorkItem.STATUS
  }
];
