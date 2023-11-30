import { baseColumnConfig } from "utils/base-table-config";
import { statusColumn } from "./common-table-columns";

export const JunitTestTableConfig = [
  {
    ...baseColumnConfig("Job Name", "job_name"),
    filterLabel: "Job Name"
  },
  {
    ...baseColumnConfig("Test Name", "test_name"),
    filterLabel: "Test Name"
  },
  {
    ...baseColumnConfig("CICD User", "cicd_user_id"),
    filterLabel: "User Name"
  },
  {
    ...baseColumnConfig("Job Run Number", "job_run_number"),
    filterLabel: "Job Run Number"
  },
  {
    ...baseColumnConfig("Duration", "duration"),
    filterLabel: "Duration"
  },
  {
    ...statusColumn("Status", "nofilter"),
    dataIndex: "status"
  },
  statusColumn("Job Status", "job_status"),
  {
    ...baseColumnConfig("Test Suite", "test_suite"),
    filterLabel: "Test Suite"
  },
  {
    ...baseColumnConfig("Test Status", "test_status", { hidden: true }),
    filterLabel: "Test Status",
    filterField: "test_status",
    description: "Just to show the filters"
  }
];
