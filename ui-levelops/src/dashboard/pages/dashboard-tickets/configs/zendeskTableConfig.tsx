import { capitalize } from "lodash";
import { baseColumnConfig } from "utils/base-table-config";
import { userColumn, statusColumn, timeColumn } from "./common-table-columns";
import React from "react";
import { AntText } from "shared-resources/components";
import { convertToDays } from "utils/timeUtils";

export const zendeskSalesForceHygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const ZendeskTableConfig = [
  userColumn("Assignee", "assignee_email"),
  userColumn("Submitter", "submitter_email"),
  userColumn("Requester", "requester_email"),
  statusColumn(),
  baseColumnConfig("Brand", "brand"),
  baseColumnConfig("Type", "type"),
  timeColumn(),
  timeColumn("Updated At", "updated_at"),
  {
    ...baseColumnConfig("Hygiene", "id1", { hidden: true }),
    filterType: "multiSelect",
    filterField: "hygiene_types",
    options: zendeskSalesForceHygieneTypes,
    filterLabel: "Hygiene"
  }
];

export const zendeskTicketIdColumn = {
  ...baseColumnConfig("Ticket ID", "ticket_id", { width: "5%" }),
  render: (item: any, record: any, index: number) => {
    return (
      <>
        {record?.ticket_url?.length > 0 ? (
          <a href={record?.ticket_url} target="_blank">
            {record?.ticket_id}
          </a>
        ) : (
          <AntText>{record?.ticket_id}</AntText>
        )}
      </>
    );
  }
};

export const zendeskResolutionTimeTableConfig = [
  zendeskTicketIdColumn,
  {
    ...baseColumnConfig("Title", "subject"),
    render: (item: any, record: any, index: number) => {
      return (
        <div>
          <p style={{ margin: "auto 0" }}>{capitalize(record?.subject)}</p>
        </div>
      );
    }
  },
  userColumn("Requester", "requester_email"),
  statusColumn(),
  {
    ...baseColumnConfig("Resolution Time (days)", "full_resolution_time"),
    render: (item: any, record: any, index: number) => {
      const time = [0, undefined, ""].includes(record?.full_resolution_time)
        ? "N/A"
        : `${convertToDays(record?.full_resolution_time)} days`;
      return (
        <div>
          <p style={{ margin: "auto 0" }}>{time}</p>
        </div>
      );
    }
  },
  {
    ...baseColumnConfig("Response Time (days)", "first_reply_time"),
    render: (item: any, record: any, index: number) => {
      const time = [0, undefined, ""].includes(record?.first_reply_time)
        ? "N/A"
        : `${convertToDays(record?.first_reply_time)} days`;
      return (
        <div>
          <p style={{ margin: "auto 0" }}>{time}</p>
        </div>
      );
    }
  }
];
