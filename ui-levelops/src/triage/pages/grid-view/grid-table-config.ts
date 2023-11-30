import { nameColumn, tableCell } from "utils/tableUtils";

export const defaultTriageGridViewColumns = [
  {
    ...nameColumn(`/`),
    filterLabel: "Name",
    filterType: "multiSelect",
    filterField: "job_ids",
    valueName: "cicd_job_id",
    fixed: "left",
    ellipsis: true,
    span: 8,
    className: "grid-view-name-col",
    children: [
      {
        name: "",
        dataIndex: "",
        title: "",
        children: [
          {
            title: "Daily Totals",
            valueName: "cicd_job_id",
            dataIndex: "name",
            width: 250,
            ellipsis: true,
            key: "name"
          }
        ]
      }
    ]
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    filterLabel: "Status",
    filterType: "multiSelect",
    filterField: "results",
    span: 8,
    render: (item: string, record: any, index: number) => {
      return tableCell("status", item ? item.toLowerCase() : "");
    },
    valueName: "job_status",
    description: "Just to show the filters",
    hidden: true
  },
  {
    title: "Triage Rules",
    key: "triage_rules",
    dataIndex: "triage_rules",
    filterType: "multiSelect",
    filterField: "triage_rule_ids",
    uri: "triage_rules",
    searchField: "name",
    valueName: "id",
    filterLabel: "Triage Rules",
    hidden: true,
    description: "Just to show the filters",
    span: 8
  },
  {
    title: "parent job id",
    filterLabel: "Parent Job name",
    filterType: "multiSelect",
    filterField: "parent_job_ids",
    valueName: "cicd_job_id",
    hidden: true,
    span: 8
  },
  {
    title: "Date Range",
    key: "date_range",
    dataIndex: "date_range",
    filterType: "dateRange",
    filterLabel: "Date between",
    filterField: "start_time",
    hidden: true,
    description: "Just to show the filters",
    span: 8
  },
  {
    title: "CI/CD User",
    key: "cicd_user_id",
    dataIndex: "cicd_user_id",
    filterLabel: "CI/CD User",
    filterType: "multiSelect",
    filterField: "cicd_user_ids",
    span: 8,
    hidden: true,
    valueName: "cicd_user_id",
    width: "20%"
  },
  {
    title: "Job Normalized Full Names",
    key: "job_normalized_full_name",
    dataIndex: "job_normalized_full_name",
    filterLabel: "Job Normalized Full Names",
    filterType: "multiSelect",
    filterField: "job_normalized_full_names",
    width: "20%",
    span: 8,
    hidden: true,
    valueName: "job_normalized_full_name"
  },
  {
    title: "Instance Name",
    key: "instance_name",
    dataIndex: "instance_name",
    filterLabel: "Instance Name",
    filterType: "multiSelect",
    filterField: "instance_names",
    width: "20%",
    span: 10,
    hidden: true,
    valueName: "instance_name"
  }
];
