import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import ErrorWrapper from "hoc/errorWrapper";
import { TableRowActions } from "shared-resources/components";
import Loader from "components/Loader/Loader";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table-config";
import { RouteComponentProps } from "react-router-dom";
import { restAPILoadingState } from "utils/stateUtil";
import { getTemplateDeleteSelector, getTemplateBulkDeleteSelector } from "reduxConfigs/selectors/templatesSelector";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { cTemplatesDelete, cTemplatesBulkDelete } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";

interface TemplatesListPageProps extends RouteComponentProps {
  className?: string;
}

export const TemplatesListPage: React.FC<TemplatesListPageProps> = (props: TemplatesListPageProps) => {
  const [delete_loading, setDeleteLoading] = useState(false);
  const [delete_template_id, setDeleteTemplateId] = useState<string | undefined>(undefined);
  const [highlightedRow, setHighlightedRow] = useState<string | undefined>(undefined);
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [rowSelection, setRowSelection] = useState<any>({});
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState<number>(1);

  const dispatch = useDispatch();
  const templateDltState = useSelector(getTemplateDeleteSelector);
  const templateBulkDeleteState = useSelector(getTemplateBulkDeleteSelector);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("ctemplates", "list", 0));
      dispatch(restapiClear("ctemplates", "delete", -1));
      dispatch(restapiClear("ctemplates", "bulkDelete", -1));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (delete_loading) {
      const { loading, error } = restAPILoadingState(templateDltState, delete_template_id);
      if (!loading) {
        if (!error) {
          //@ts-ignore
          const data = get(templateDltState, [delete_template_id, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            setSelectedIds((ids: string[]) => ids.filter(id => id !== delete_template_id));
          }
        }
        setDeleteLoading(false);
        setDeleteTemplateId(undefined);
      }
    }
  }, [templateDltState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = templateBulkDeleteState["0"];
      if (!loading) {
        if (!error) {
          const data = get(templateBulkDeleteState, ["0", "data", "records"], []);
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
  }, [templateBulkDeleteState, bulkDeleting]); // eslint-disable-line react-hooks/exhaustive-deps

  const onRemoveHandler = (templateId: string) => {
    setDeleteLoading(true);
    setDeleteTemplateId(templateId);
    dispatch(cTemplatesDelete(templateId));
  };

  const onRowClick = (e: any, t: any, rowInfo: any) => {
    setHighlightedRow(rowInfo.original.id);
  };

  const buildActionOptions = (actionProps: { id: string; default: boolean }) => {
    const actions = actionProps.default
      ? []
      : [
          {
            type: "delete",
            id: actionProps.id,
            onClickEvent: onRemoveHandler
          }
        ];
    return <TableRowActions actions={actions} />;
  };

  const PAGE_ERROR = "Could not fetch templates";

  const mappedColumns = tableColumns().map(column => {
    if (column.key === "id") {
      return {
        ...column,
        render: (item: string, record: any, index: number) => buildActionOptions(record)
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
      onChange: onSelectChange,
      getCheckboxProps: (records: any) => ({
        disabled: records.default
      })
    });
  }, [selectedIds]); // eslint-disable-line react-hooks/exhaustive-deps

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  if (delete_loading) {
    return <Loader />;
  }

  const onBulkDelete = () => {
    dispatch(cTemplatesBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  return (
    <>
      <ServerPaginatedTable
        pageName={"ctemplates"}
        restCall="getCTemplates"
        uri="ctemplates"
        backendErrorMessage={PAGE_ERROR}
        getTrProps={(state: any, rowInfo: any, column: any) => {
          return {
            onMouseOver: (e: any, t: any) => {
              onRowClick(e, t, rowInfo);
            }
          };
        }}
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

export default ErrorWrapper(React.memo(TemplatesListPage));
