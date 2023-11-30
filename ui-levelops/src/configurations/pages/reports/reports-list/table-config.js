import { tableCell } from "utils/tableUtils";

export const tableConfig = [
  {
    title: "Report",
    dataIndex: "name",
    key: "name",
    width: 300
    // filterType: 'search',
    // filterField: 'name'
  },
  {
    title: "Type",
    key: "type",
    dataIndex: "type",
    width: 100,
    filterType: "select",
    filterField: "type",
    options: ["type1", "type2", "type3"],
    filterLabel: "Type"
  },
  {
    title: "Products",
    dataIndex: "products",
    key: "products",
    width: 200,
    render: props => tableCell("products", props),
    filterType: "apiSelect",
    filterField: "product_id",
    uri: "products",
    searchField: "name"
  },
  {
    title: "Tags",
    dataIndex: "tags",
    key: "tags",
    width: 200,
    render: props => tableCell("tags", props),
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    uri: "tags",
    searchField: "name"
  },
  {
    title: "Created At",
    dataIndex: "created_on",
    key: "created_on",
    width: 200,
    render: props => tableCell("created_at", props)
  }
];
