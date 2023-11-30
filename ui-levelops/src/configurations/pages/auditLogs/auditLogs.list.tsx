import React, { useMemo } from "react";
import { RouteComponentProps } from "react-router-dom";
import { tableColumns } from "./tableConfig";
import { ServerPaginatedTable } from "shared-resources/containers";
import { Empty } from "antd";
import { AntText } from "shared-resources/components";
import ExpandedDetailsContainer from "./expandedDetails";

interface RunsListContainerProps extends RouteComponentProps {}

const AuditLogs: React.FC<RunsListContainerProps> = (props: RunsListContainerProps) => {
  return (
    <ServerPaginatedTable
      pageName={"Activity Logs"}
      title={"Activity Logs"}
      uri={"activitylogs"}
      hasFilters
      uuid={"audit_logs"}
      columns={tableColumns}
      hasSearch={false}
      expandedRowRender={(record: any) => <ExpandedDetailsContainer record={record} />}
    />
  );
};

export default AuditLogs;
