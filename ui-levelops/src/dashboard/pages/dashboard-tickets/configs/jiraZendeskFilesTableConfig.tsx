import { forEach, isArray, map, last } from "lodash";
import React from "react";
import { AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { dateRangeFilterColumn } from "./common-table-columns";

export const JiraZendeskFilesTableConfig = [
  baseColumnConfig("Repo", "repo_id"),
  baseColumnConfig("Filename", "filename"),
  {
    ...baseColumnConfig("Zendesk Tickets", "zendesk_ticket_ids"),
    render: (item: any, record: any) => {
      return (
        <>
          {isArray(item) &&
            map(item, ticketId => {
              let url: string | undefined = undefined;
              forEach(record?.zendesk_ticket_urls || [], ticketUrl => {
                const chunks = ticketUrl.split("/");
                if (last(chunks) === ticketId) {
                  url = ticketUrl;
                }
              });
              return (
                <>
                  {url !== undefined ? (
                    <a href={url} target="_blank">
                      {ticketId} {"  "}
                    </a>
                  ) : (
                    <AntText>
                      {ticketId} {"  "}
                    </AntText>
                  )}
                </>
              );
            })}
        </>
      );
    }
  },
  baseColumnConfig("Num Commits", "num_commits"),
  baseColumnConfig("Total Deletions", "total_deletions"),
  baseColumnConfig("Total Additions", "total_additions"),
  baseColumnConfig("Total Changes", "total_changes"),
  {
    ...dateRangeFilterColumn("Issue Created At", "jira_issue_created_at"),
    description: "Just to show the filters"
  },
  {
    ...dateRangeFilterColumn("Issue Updated At", "jira_issue_updated_at"),
    description: "Just to show the filters"
  }
];
