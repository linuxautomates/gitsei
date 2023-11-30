import React from "react";
import { ServerPaginatedTable } from "../../../shared-resources/containers";

interface ChildTicketTableProps {
  integrationIds: Array<string>;
  columns: Array<any>;
  uri: string;
  ticketId: string;
}

const ChildTicketTable: React.FC<ChildTicketTableProps> = (props: ChildTicketTableProps) => {
  const { ticketId, uri, integrationIds, columns } = props;
  const key = uri === "issue_management_list" ? "workitem_epics" : "epics";
  return (
    <ServerPaginatedTable
      uuid={`child-tickets-${ticketId}`}
      uri={uri}
      columns={columns}
      hasSearch={false}
      hasFilters={false}
      moreFilters={{ [key]: [ticketId], integration_ids: integrationIds }}
    />
  );
};

export default ChildTicketTable;
