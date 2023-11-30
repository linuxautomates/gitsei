import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { TableRowActions } from "shared-resources/components";
import Loader from "components/Loader/Loader";
import ErrorWrapper from "hoc/errorWrapper";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table-config";
import { bpsDelete, restapiClear, bpsBulkDelete } from "reduxConfigs/actions/restapi";
import { KbsSelectorState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";

export enum KbType {
  LINK = "LINK",
  TEXT = "TEXT",
  FILE = "FILE"
}

export interface KBListPageProps {
  rest_api: any;
  restapiClear: Function;
  history: any;
  id: number | string;
  type: KbType;
  value: string;
  name: string;
  tagsList: any;
  moreFilters: any;
}

const KBListPage: React.FC<KBListPageProps> = (props: KBListPageProps) => {
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [deletingKbId, setDeletingKbId] = useState<number | string>();
  const dispatch = useDispatch();
  const apiState = useSelector(state => KbsSelectorState(state));
  const [selectedIds, setSelectedIds] = useState<any>([]);

  const [rowSelection, setRowSelection] = useState<any>({});
  const [reload, setReload] = useState<number>(1);
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);

  useEffect(() => {
    return () => {
      // @ts-ignore
      dispatch(restapiClear("bestpractices", "list", "0"));

      // @ts-ignore
      dispatch(restapiClear("bestpractices", "delete", "-1"));

      // @ts-ignore
      dispatch(restapiClear("tags", "list", "0"));

      // @ts-ignore
      dispatch(restapiClear("tags", "bulk", "0"));

      // @ts-ignore
      dispatch(restapiClear("bestpractices", "bulkDelete", "-1"));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (deleteLoading && deletingKbId) {
      const loading = get(apiState, ["delete", deletingKbId, "loading"], true);
      const error = get(apiState, ["delete", deletingKbId, "error"], true);
      if (!loading) {
        if (!error) {
          const data = get(apiState, ["delete", deletingKbId, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            setSelectedIds((ids: string[]) => ids.filter(id => id !== deletingKbId));
          }
        }
        setDeleteLoading(false);
        setDeletingKbId(undefined);
      }
    }
  }, [apiState?.delete]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = apiState.bulkDelete[0];
      if (!loading) {
        if (!error) {
          const data = get(apiState, ["bulkDelete", "0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(state => state + 1);
          } else {
            setSelectedIds([]);
            setReload(state => state + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [apiState?.bulkDelete]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (deleteLoading && deletingKbId) {
      dispatch(bpsDelete(deletingKbId));
    }
  }, [deleteLoading, deletingKbId]); // eslint-disable-line react-hooks/exhaustive-deps

  function onRemoveHandler(kbId: string | number) {
    setDeletingKbId(kbId);
    setDeleteLoading(true);
  }

  function buildActionOptions(id: number | string) {
    const actions: any = [
      {
        type: "delete",
        id,
        onClickEvent: onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  }

  const mappedColumns = tableColumns().map(column => {
    if (column.key === "tags") {
      return {
        ...column,
        apiCall: props.tagsList
      };
    }
    if (column.dataIndex === "id") {
      return {
        ...column,
        render: (item: any, record: any, index: any) => buildActionOptions(record?.id)
      };
    }
    return column;
  });

  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  useEffect(() => {
    setRowSelection({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange
    });
  }, [selectedIds]); // eslint-disable-line react-hooks/exhaustive-deps

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const onBulkDelete = () => {
    dispatch(bpsBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  return deleteLoading ? (
    <Loader />
  ) : (
    <>
      <ServerPaginatedTable
        pageName={"bestpractices"}
        restCall="getBps"
        uri="bestpractices"
        moreFilters={props.moreFilters || {}}
        columns={mappedColumns}
        hasFilters={false}
        clearSelectedIds={clearSelectedIds}
        rowSelection={rowSelection}
        onBulkDelete={onBulkDelete}
        reload={reload}
        hasDelete={true}
        bulkDeleting={bulkDeleting}
      />
    </>
  );
};

export default ErrorWrapper(KBListPage);
