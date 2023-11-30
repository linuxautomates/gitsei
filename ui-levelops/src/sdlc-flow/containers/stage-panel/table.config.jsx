import React from "react";
import { Icon } from "antd";
import { SvgIcon } from "shared-resources/components";

export const stageColumns = [
  {
    title: "Name",
    dataIndex: "name",
    key: "name"
  },
  {
    title: "Source",
    dataIndex: "source",
    key: "source",
    render: value => <SvgIcon icon={value} style={{ width: "2rem", height: "2rem" }} />
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: value => (
      <Icon
        type={value ? "check-circle" : "close-circle"}
        theme="filled"
        style={{ color: value ? "green" : "red", fontSize: "2.1rem" }}
      />
    )
  },
  {
    title: "Report",
    dataIndex: "report_id",
    key: "report",
    render: value => (
      <a
        href={`${window.location.origin}/reports?report_id=${value}`}
        style={{ color: "var(--link-and-actions" }}
        // eslint-disable-next-line react/jsx-no-target-blank
        target="_blank">
        View report
      </a>
    )
  }
];
