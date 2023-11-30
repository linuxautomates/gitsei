import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableCell } from "utils/tableUtils";
import ErrorWrapper from "hoc/errorWrapper";
import { AntCard } from "shared-resources/components";
import { tableColumns } from "./table-config";
import { restapiClear } from "reduxConfigs/actions/restapi";

interface ViolationLogsListPageProps {}
export const ViolationLogsListPage: React.FC<ViolationLogsListPageProps> = (props: ViolationLogsListPageProps) => {
  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(restapiClear("event_logs", "list", "0"));
    }; // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onRowClickHandler = (e: any, t: any, rowInfo: any) => {};

  const PAGE_ERROR = "Could not fetch violation logs";

  const columns = tableColumns.map(column => {
    if (column.key === "created_at") {
      return {
        ...column,
        render: (props: any) => tableCell("created_at", props)
      };
    }
    return column;
  });

  return (
    <AntCard title="Violation Logs">
      <ServerPaginatedTable
        restCall="getEventLogs"
        uri={"event_logs"}
        backendErrorMessage={PAGE_ERROR}
        columns={columns}
        getTrProps={(state: any, rowInfo: any, column: any) => {
          return {
            onMouseOver: (e: any, t: any) => {
              onRowClickHandler(e, t, rowInfo);
            }
          };
        }}
        partialFilters={false}
        hasFilters={false}
        generalSearchField="policy"
      />
    </AntCard>
  );
};

export default React.memo(ErrorWrapper(ViolationLogsListPage));
