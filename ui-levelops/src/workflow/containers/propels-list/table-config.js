import { tableCell, actionsColumn, updatedAtColumn, nameColumn } from "utils/tableUtils";
import { getBaseUrl } from 'constants/routePaths'

export const tableColumns = () => [
  { ...nameColumn(`${getBaseUrl()}/propels/propels-editor?propel`, "name", 250, "permanent_id") },
  { ...updatedAtColumn() },
  {
    title: "Enabled",
    key: "enabled",
    dataIndex: "enabled",
    filterable: false,
    sortable: false,
    width: 100,
    filterType: "select",
    filterField: "enabled",
    filterLabel: "Enabled",
    options: [
      { label: "Enabled", value: true, key: true },
      { label: "Disabled", value: false, key: false }
    ],
    render: (item, record, index) => tableCell("enabled", item),
    span: 8
  },
  {
    title: "Trigger",
    key: "trigger_template_type",
    dataIndex: "trigger_template_type",
    filterable: false,
    sortable: false,
    width: "10%",
    render: (item, record, index) => (item ? item.replace(/_/g, " ") : ""),
    filterType: "apiSelect",
    filterField: "trigger_template_type",
    uri: "propel_trigger_templates",
    filterLabel: "Trigger",
    searchField: "display_name",
    specialKey: "type",
    span: 8
  },
  { ...actionsColumn() }
];
