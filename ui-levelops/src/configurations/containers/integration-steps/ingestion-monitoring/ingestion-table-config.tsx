import React from "react";
import { baseColumnConfig } from "../../../../utils/base-table-config";
import moment from "moment";
import { getTimeToComplete, getScanTimeRange } from "./table.helper";
import { StatusWithIcon, AntIcon } from "shared-resources/components";
import ErrorDetailsContainer from "./error-details.container";
import { Tooltip } from "antd";

const statusText = {
  success: "Success",
  failure: "Failed",
  pending: "Pending",
  scheduled: "Scheduled"
};

export const IngestionLogTableConfig = [
  {
    ...baseColumnConfig("Scan Range Time", "scan_range_time", { width: "5%" }),
    render: (item: string, record: any) => getScanTimeRange(record) || "Unknown",
    title: (
      <Tooltip title={"Time period for which data is being fetched."}>
        Scan Range Time <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  },
  {
    ...baseColumnConfig("Data Retrieval Process", "created_at", { width: "4%" }),
    render: (item: number) => "Ingestion",
    title: (
      <Tooltip
        title={
          "Data retrieval process is the method of fetching data via integration into SEI. Latest aggregation is the status of the last aggregation done out of the multiple aggregations for the ingestion."
        }>
        Data Retrieval Process <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  },
  {
    ...baseColumnConfig("Task Start Time", "created_at", { width: "5%" }),
    render: (item: number) => moment.unix(item).format() || "Unknown",
    title: (
      <Tooltip title={"When the task/job begins to execute."}>
        Task Start Time <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  },
  {
    ...baseColumnConfig("Status", "status", { width: "3%" }),
    render: (value: "success" | "failed" | "failure" | "pending" | "scheduled", record: any) => {
      if (value === "failure") {
        return <ErrorDetailsContainer integrationObj={record} status={value} text={statusText} />;
      } else {
        return <StatusWithIcon status={value} text={statusText} />;
      }
    },
    filterField: "statuses",
    filterType: "multiSelect",
    options: [
      { value: "success", label: "Success" },
      { value: "failure", label: "Failed" },
      { value: "pending", label: "Pending" },
      { value: "scheduled", label: "Scheduled" }
    ],
    span: 8,
    title: (
      <Tooltip title={"Log status for ingestion/aggregation."}>
        Status <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  },
  {
    ...baseColumnConfig("Time to complete", "elapsed", { width: "3%" }),
    render: (value: number) => getTimeToComplete(value) || "Unknown",
    title: (
      <Tooltip title={"The total time it took to finish the execution."}>
        Time to complete <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  },
  {
    ...baseColumnConfig("Retries", "attempt_count", { width: "3%" }),
    title: (
      <Tooltip title={"No. of times the task/job was executed."}>
        Retries <AntIcon className="info-icon" type="info-circle" style={{ color: "#0278D5" }} />
      </Tooltip>
    )
  }
];
