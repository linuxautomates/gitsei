import { tableCell } from "../../../../utils/tableUtils";

export const tableColumns = [
  {
    title: "  ",
    key: "id",
    dataIndex: "id",
    width: 50
  },
  {
    title: "Name",
    key: "name",
    dataIndex: "name",
    ellipsis: true,
    width: 350
  },
  {
    title: "Tags",
    key: "tags",
    dataIndex: "tags",
    render: props => tableCell("tags", props === undefined || props === null ? [] : props),
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    searchField: "name",
    uri: "tags"
  }
];
