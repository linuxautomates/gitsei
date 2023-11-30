import { tableCell, nameColumn, updatedAtColumn, actionsColumn } from "utils/tableUtils";
import { getBaseUrl, TEMPLATE_ROUTES } from "constants/routePaths";

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}${TEMPLATE_ROUTES.ASSESSMENT_TEMPLATES.EDIT}?questionnaire`) },
  { ...updatedAtColumn() },
  {
    title: "Tags",
    filterTitle: "Tags",
    key: "tag_ids",
    dataIndex: "tag_ids",
    filterType: "apiMultiSelect",
    filterField: "tag_ids",
    uri: "tags",
    width: 300,
    render: props => tableCell("tags", props),
    span: 8
  },
  { ...actionsColumn() }
];
