import { tableCell, nameColumn, updatedAtColumn, actionsColumn } from "utils/tableUtils";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}${TEMPLATE_ROUTES.KB.EDIT}?kb`) },
  { ...updatedAtColumn() },
  {
    title: "Type",
    dataIndex: "type",
    key: "type",
    width: 50
  },
  {
    title: "Tags",
    dataIndex: "tags",
    key: "tags",
    render: props => tableCell("tags", props === undefined || props === null ? [] : props),
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    searchField: "name",
    uri: "tags",
    width: "20%"
  },
  { ...actionsColumn() }
];
