import React, { useCallback, useEffect, useMemo, useState } from "react";
import { propelRunsTableConfig } from "reports/components/table-config";
import { ServerPaginatedTable } from "shared-resources/containers";
import { useDispatch, useSelector } from "react-redux";
import { propelReportsBulkDelete } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { propelBulkDeleteSelector } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";

interface PropelReportsListProps {
  moreFilters?: any;
  partialFilters?: any;
}

const PropelReportsListComponent: React.FC<PropelReportsListProps> = ({ moreFilters, partialFilters }) => {
  const dispatch = useDispatch();
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [reload, setReload] = useState<number>(0);
  const [bulkDeleting, setBulkDeleting] = useState(false);

  const propelBulkDeleteState = useSelector(state => propelBulkDeleteSelector(state));

  useEffect(() => {
    return () => {
      dispatch(restapiClear("propel_reports", "bulkDelete", "-1"));
    };
  }, []);

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = get(propelBulkDeleteState, ["0"], { loading: true, error: true });
      if (!loading) {
        if (!error) {
          const data = get(propelBulkDeleteState, ["0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(reload => reload + 1);
          } else {
            setSelectedIds([]);
            setReload(reload => reload + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [propelBulkDeleteState]);

  const clearSelectedIds = useCallback(() => setSelectedIds([]), []);

  const onSelectChange = useCallback((rowKeys: string[]) => setSelectedIds(rowKeys), []);

  const onBulkDelete = useCallback(() => {
    dispatch(propelReportsBulkDelete(selectedIds));
    setBulkDeleting(true);
  }, [selectedIds]);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange,
      hideDefaultSelections: false
    }),
    [selectedIds]
  );

  return (
    <ServerPaginatedTable
      uri={"propel_reports"}
      displayCount={false}
      recordName="Propel Reports"
      moreFilters={moreFilters || {}}
      partialFilters={partialFilters || {}}
      columns={propelRunsTableConfig()}
      hasSearch={false}
      hasFilters={false}
      rowKey={"id"}
      rowSelection={rowSelection}
      clearSelectedIds={clearSelectedIds}
      bulkDeleting={bulkDeleting}
      onBulkDelete={onBulkDelete}
      reload={reload}
      hasDelete={true}
    />
  );
};

export default React.memo(PropelReportsListComponent);
