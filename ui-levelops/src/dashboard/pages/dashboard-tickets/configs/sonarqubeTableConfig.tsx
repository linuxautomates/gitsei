import React from "react";
import { baseColumnConfig } from "utils/base-table-config";
import { statusColumn, priorityColumn, userColumn } from "./common-table-columns";

export const sonarqubeMetricsTableConfig = [
  baseColumnConfig("Project", "project"),
  baseColumnConfig("Organization", "organization"),
  baseColumnConfig("Visibility", "visibility"),
  baseColumnConfig("Pull Request", "pull_request"),
  baseColumnConfig("Pr Branch", "pr_branch"),
  baseColumnConfig("Pr Target Branch", "pr_target_branch"),
  baseColumnConfig("Pr Base Branch", "pr_base_branch")
];

export const sonarqubeTableConfig = [
  {
    ...baseColumnConfig("Issue", "project", { width: "20%" }),
    render: (item: any, record: any) => {
      const baseURL =
        ((record?.base_url ?? "") as string).lastIndexOf("/") === record?.base_url?.length - 1
          ? record?.base_url
          : `${record?.base_url}/`;

      const _url = `${baseURL}project/issues?open=${record.key}&id=${record.project}`;
      return (
        <a href={_url} target={"_blank"}>
          {record.key}
        </a>
      );
    }
  },
  baseColumnConfig("Effort", "effort"),
  statusColumn("Status", "status"),
  baseColumnConfig("Severity", "severity"),
  baseColumnConfig("Organization", "organization"),
  userColumn("Author", "author"),
  baseColumnConfig("Type", "type"),
  baseColumnConfig("Project", "project")
];
