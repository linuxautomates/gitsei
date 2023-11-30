import { tableCell } from "utils/tableUtils";
import { RestWorkItem } from "classes/RestWorkItem";
import React from "react";

export const tableColumns = [
  {
    title: "Policy",
    dataIndex: "policy_name",
    key: "policy_name",
    width: 300,
    filterType: "apiSelect",
    filterField: "policy_id",
    uri: "policies",
    filterLabel: "Policy"
  },
  {
    title: "Artifact",
    dataIndex: "artifact",
    key: "artifact",
    width: 100,
    filterType: "search",
    filterField: "artifact"
  },
  {
    title: "Title",
    dataIndex: "artifact_title",
    key: "artifact_title",
    width: 400
  },
  {
    title: "Assignees",
    dataIndex: "assignees",
    key: "assignees",
    render: (item, record, index) => {
      if (Array.isArray(item)) {
        if (item.length > 0) {
          return item.map(user => user.user_email).join(",");
        }
      }
      return "UNASSIGNED";
    },
    width: 200,
    filterType: "apiSelect",
    filterField: "assignee_email",
    uri: "users",
    searchField: "email",
    specialKey: "email"
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    render: props => tableCell("status", props),
    width: 120,
    filterType: "select",
    filterField: "status",
    options: RestWorkItem.STATUS
  },
  {
    title: "Risk",
    key: "priority",
    dataIndex: "priority",
    render: props => tableCell("priority", props),
    width: 100,
    filterType: "select",
    filterField: "priority",
    options: RestWorkItem.RISKS,
    filterLabel: "Risk"
  }
  // {
  //   title: "type",
  //   key: "type",
  //   dataIndex: "type",
  //   filterable: false,
  //   sortable:false,
  //   style: {textAlign: "left"},
  //   headerStyle: {
  //     textAlign: "left"
  //   }
  // },
  // {
  //   title: "Due Date",
  //   key: "due_at",
  //   dataIndex: "due_at",
  //   filterable: false,
  //   style: {textAlign: "left"},
  //   headerStyle: {
  //     textAlign: "left"
  //   },
  //   render: (props) => (tableCell("due_at",props))
  // },
  // {
  //   title: "Created At",
  //   key: "created_at",
  //   dataIndex: "created_at",
  //   filterable: false,
  //   headerStyle: {
  //     textAlign: "left"
  //   },
  //   render: (props) => (tableCell("created_at",props))
  // },
];
