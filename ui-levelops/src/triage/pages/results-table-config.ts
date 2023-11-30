// @ts-ignore
import { nameColumn, tableCell } from "utils/tableUtils";
import { getBaseUrl, TRIAGE_ROUTES } from "../../constants/routePaths";
import { convertEpochToDate } from "../../utils/dateUtils";

export const tableColumns = () => [
  {
    ...nameColumn(`${getBaseUrl()}${TRIAGE_ROUTES.DETAIL}?id`, "job_name"),
    filterLabel: "Name",
    filterType: "multiSelect",
    filterField: "job_names",
    valueName: "job_name",
    width: "30%",
    span: 8
  },
  {
    title: "Run Number",
    key: "job_run_number",
    dataIndex: "job_run_number",
    width: 100
  },
  {
    title: "Updated On (GMT)",
    key: "start_time",
    dataIndex: "start_time",
    width: "10%",
    sorter: true,
    render: (value: number, record: any, index: number) => {
      return convertEpochToDate(value / 1000, "MM/DD/YYYY", true);
      //return tableCell("created_at", getStartOfDayTimeStampGMT(value));
    },
    span: 8
  },
  {
    title: "Result",
    key: "status",
    dataIndex: "status",
    filterLabel: "Status",
    filterType: "multiSelect",
    filterField: "job_statuses",
    width: "10%",
    span: 8,
    render: (item: string, record: any, index: number) => {
      return tableCell("status", item ? item.toLowerCase() : "");
    },
    valueName: "job_status"
  },
  {
    title: "CI/CD User",
    key: "cicd_user_id",
    dataIndex: "cicd_user_id",
    filterLabel: "CI/CD User",
    filterType: "multiSelect",
    filterField: "cicd_user_ids",
    span: 8,
    valueName: "cicd_user_id",
    width: "20%"
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
    title: "Instance Name",
    key: "instance_name",
    dataIndex: "instance_name",
    filterLabel: "Instance Name",
    filterType: "multiSelect",
    filterField: "instance_names",
    width: "20%",
    span: 8,
    hidden: true,
    valueName: "instance_name"
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
  }
];
