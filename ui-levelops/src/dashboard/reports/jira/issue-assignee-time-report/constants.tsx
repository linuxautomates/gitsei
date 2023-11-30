import React from "react";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import moment from "moment";
import { JiraIssueLink } from "shared-resources/components/jira-issue-link/jira-issue-link-component";

export const issueFirstAssigneeChartProps = {
  size: "small",
  columns: [
    {
      title: "Assignee",
      key: "assignee",
      dataIndex: "assignee",
      width: "25%"
    },
    {
      title: "Issue",
      key: "key",
      dataIndex: "key",
      width: "25%",
      render: (item: string, record: basicMappingType<string>) => {
        const url = `ticket_details`.concat(`?key=${record?.key}&integration_id=${record?.integration_id}`);
        return <JiraIssueLink link={url} ticketKey={item} integrationUrl={record?.integration_url} />;
      }
    },
    {
      title: "Summary",
      key: "summary",
      dataIndex: "summary",
      ellipsis: true,
      width: "25%"
    },
    {
      title: "Total time",
      key: "total_time",
      dataIndex: "total_time",
      width: "25%",
      render: (item: number) => moment.duration(item, "seconds").format()
    }
  ],
  chartProps: {}
};

export const issueAssigneeTimeHiddenFilters = {
  exclude: {
    assignees: ["_UNASSIGNED_"],
    time_assignees: ["_UNASSIGNED_"]
  }
};
