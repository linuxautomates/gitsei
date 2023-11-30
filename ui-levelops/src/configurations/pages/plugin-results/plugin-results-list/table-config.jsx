import { tableCell } from "utils/tableUtils";
import React from "react";
import { AntText } from "shared-resources/components";
import { WORKSPACES } from "dashboard/constants/applications/names";
import { getSettingsPage, getBaseUrl } from 'constants/routePaths'

export const tableColumns = [
  {
    title: "Plugin",
    key: "plugin_name",
    dataIndex: "plugin_name",
    render: (item, record, index) => {
      const id = record.id;
      const _url =
        record.class !== "report_file"
          ? `${getSettingsPage()}/plugin-results-details?result=${id}`
          : `${getBaseUrl()}/reports-view?report=${id}`;
      return (
        <AntText style={{ paddingLeft: "10px" }}>
          <a className={"ellipsis"} href={_url}>
            {item}
          </a>
        </AntText>
      );
    }
  },
  {
    title: "Created At",
    key: "created_at_epoch",
    dataIndex: "created_at_epoch",
    render: value => tableCell("created_at", value)
  },
  {
    title: "Tags",
    filterTitle: "Tags",
    key: "tags",
    dataIndex: "tags",
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    uri: "tags",
    render: props => tableCell("tags", props),
    span: 8
  },
  {
    title: "Version",
    key: "version",
    dataIndex: "version"
  },
  {
    title: "Success",
    dataIndex: "successful",
    key: "successful",
    render: value => tableCell("successful", value)
  },
  {
    title: WORKSPACES,
    dataIndex: "workspaces",
    key: "workspaces",
    filterType: "apiMultiSelect",
    filterField: "product_ids",
    uri: "workspace",
    filterLabel: WORKSPACES,
    render: value => tableCell("workspaces", [value]),
    span: 8
  },
  {
    title: "Actions",
    dataIndex: "id",
    key: "id",
    width: 100,
    filterType: "apiMultiSelect",
    filterField: "ids",
    uri: "plugins",
    filterLabel: "Tools",
    hidden: true,
    span: 8
  }
];
