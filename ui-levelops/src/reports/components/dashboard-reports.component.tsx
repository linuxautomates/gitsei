import { notification } from "antd";
import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { dashboardReportsDelete, filesGet } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { dashboardReportsTableConfig } from "reports/components/table-config";
import { TableRowActions } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";

const DashboardReportsComponent = () => {
  const [reload, setReload] = useState<number>(1);
  const deleteId = useRef<string | undefined>();
  const dispatch = useDispatch();
  const dashboardReportsDeleteState = useParamSelector(getGenericUUIDSelector, {
    uri: "dashboard_reports",
    method: "delete",
    uuid: deleteId.current
  });

  useEffect(() => {
    if (deleteId.current) {
      const loading = get(dashboardReportsDeleteState, ["loading"], true);
      const error = get(dashboardReportsDeleteState, ["error"], false);
      if (!loading) {
        if (error) {
          notification.error({ message: "Deleting Dashboard Report failed" });
        }
        setReload(prev => prev + 1);
        deleteId.current = undefined;
      }
    }
  }, [dashboardReportsDeleteState]);

  const onEditHandler = useCallback((obj: any) => {
    const dashboardId = get(obj, ["dashboard_id"], "0");
    const fileId = get(obj, ["file_id"], undefined);
    if (fileId) {
      const fileLink = `dashboards/${dashboardId}/${fileId}`;
      dispatch(filesGet(fileLink));
    }
  }, []);

  const onDeleteHandler = useCallback((dashboardId: string) => {
    dispatch(dashboardReportsDelete(dashboardId));
    deleteId.current = dashboardId;
  }, []);

  const buildActionOptions = useMemo(
    () => (record: any) => {
      const actions = [
        {
          type: "download",
          id: { file_id: record?.file_id, dashboard_id: record?.dashboard_id },
          onClickEvent: onEditHandler
        },
        {
          type: "delete",
          id: record?.id,
          onClickEvent: onDeleteHandler
        }
      ];
      return <TableRowActions actions={actions} />;
    },
    []
  );

  const mappedColumns = useMemo(
    () => [
      ...dashboardReportsTableConfig,
      {
        title: "Actions",
        key: "id",
        width: 100,
        render: (item: any, record: any, index: number) => buildActionOptions(record)
      }
    ],
    []
  );

  return (
    <ServerPaginatedTable
      uri={"dashboard_reports"}
      displayCount={false}
      recordName="Dashboard Reports"
      columns={mappedColumns}
      hasSearch={true}
      hasFilters={true}
      rowKey={"id"}
      reload={reload}
    />
  );
};

export default DashboardReportsComponent;
