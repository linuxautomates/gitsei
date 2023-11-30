import { tableCell } from "utils/tableUtils";

export const tableColumns = [
  {
    title: "Name",
    dataIndex: "name",
    key: "name",
    ellipsis: true
  },
  {
    title: "Tags",
    dataIndex: "tags",
    key: "tags",
    width: 200,
    render: props => tableCell("tags", props === undefined || props === null ? [] : props),
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    searchField: "name",
    uri: "tags"
  },
  {
    title: "Created At",
    dataIndex: "created_at",
    key: "created_at",
    width: 200,
    render: props => tableCell("created_at", props)
  }
];
