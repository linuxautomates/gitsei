import { nameColumn, tableCell, updatedAtColumn } from "../../utils/tableUtils";
import StringsEn from "../../locales/StringsEn";
import { getBaseUrl } from 'constants/routePaths'

export const tableConfig = [
  {
    title: "Created At",
    key: "created_at",
    dataIndex: "created_at",
    render: value => tableCell("updated_on", value)
  },
  {
    title: "Project",
    filterTitle: "Project",
    key: "workspaces",
    dataIndex: "workspaces",
    filterType: "apiSelect",
    filterField: "product_id",
    uri: "workspace",
    span: 8
  },
  {
    title: "Integrations",
    filterTitle: "Integration",
    key: "integrations",
    dataIndex: "integrations",
    filterType: "apiMultiSelect",
    filterField: "integration_ids",
    uri: "integrations",
    span: 8
  },
  {
    title: "Type",
    key: "type",
    dataIndex: "type"
    //render: (value) => tableCell("integration_type", value)
  },
  {
    title: "Success",
    key: "successful",
    dataIndex: "successful",
    render: value => tableCell("successful", value)
  },
  {
    title: "Actions",
    key: "id",
    width: 100
  }
];

export const propelRunsTableConfig = () => [
  { ...nameColumn(`${getBaseUrl()}/reports/propel-report?report`, "title") },
  {
    ...updatedAtColumn("created_at")
  },
  {
    title: StringsEn.propel,
    filterTitle: StringsEn.propel,
    key: "propel_name",
    dataIndex: "propel_name",
    width: "20%"
  }
];

export const dashboardReportsTableConfig = [
  {
    title: "Name",
    key: "name",
    dataIndex: "name",
    width: "25%",
    ellipsis: true
  },
  {
    title: "Dashboard",
    key: "dashboard_name",
    dataIndex: "dashboard_name",
    filterType: "apiSelect",
    filterField: "dashboard_id",
    uri: "dashboards",
    width: "25%",
    ellipsis: true
  },
  {
    title: "Created By",
    key: "created_name",
    dataIndex: "created_name",
    width: "15%",
    ellipsis: true
  },
  {
    ...updatedAtColumn("created_at"),
    sorter: true,
    width: "100",
    title: "Created On"
  }
];
