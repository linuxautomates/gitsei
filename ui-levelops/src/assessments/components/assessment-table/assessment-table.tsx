import { notification, Spin } from "antd";
import { getQuizAnswerPage } from "constants/routePaths";
import { CSVDownloadSagaType } from "dashboard/helpers/helper";
import { capitalize, get, map } from "lodash";
import React, { isValidElement, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { quizDelete } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import LocalStoreService from "services/localStoreService";
import { TableRowActions } from "shared-resources/components";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { tableConfig } from "./table-config";
import { levelopsAssessmentCountReportCSVTransformer } from "dashboard/helpers/csv-transformers/levelops-reports/assessment-count-report.transformer";
import { JSXHeadersType } from "dashboard/helpers/JSXHeaders.type";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface AssessmentTableProps {
  onSelectRows: (rowIds: any[]) => void;
  isCompact?: boolean;
  moreFilters?: any;
  partialFilters?: any;
}

const queryParamsToParse = [
  "questionnaire_template_ids",
  "created_at",
  "updated_at",
  "completed",
  "tag_ids",
  "priority",
  "created_end",
  "updated_end",
  "assignee_user_ids"
];

const AssessmentTable: React.FC<AssessmentTableProps> = (props: AssessmentTableProps) => {
  const { onSelectRows, isCompact } = props;

  const ls = new LocalStoreService();

  const deleteId = useRef<string | undefined>();
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [selectedRows, setSelectedRows] = useState<any[]>([]);

  const dispatch = useDispatch();
  const quizDeleteRestState = useParamSelector(getGenericUUIDSelector, {
    uri: "quiz",
    method: "delete",
    uuid: deleteId.current?.toString()
  });

  const access = useConfigScreenPermissions();

  useEffect(() => {
    if (deleteLoading) {
      const loading = get(quizDeleteRestState, ["loading"], true);
      const error = get(quizDeleteRestState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(quizDeleteRestState, ["data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            const _deleteId = deleteId.current;
            setSelectedRows((ids: string[]) => ids.filter(id => id !== _deleteId));
          }
        }
        deleteId.current = undefined;
        setDeleteLoading(false);
      }
    }
  }, [quizDeleteRestState]);

  const onEditHandler = useCallback((qId: any) => {
    let url = `${getQuizAnswerPage()}?questionnaire=${qId}`;
    window.location.href = url;
  }, []);

  const onRemoveHandler = useCallback((qId: any) => {
    deleteId.current = qId;
    dispatch(quizDelete(qId));
    setDeleteLoading(true);
  }, []);

  const buildActions = useCallback((props: any) => {
    const rbac = ls.getUserRbac();
    let actions = [
      {
        type: "eye",
        id: props.id,
        onClickEvent: onEditHandler
      }
    ];
    const deleteAccess = window.isStandaloneApp
      ? getRBACPermission(PermeableMetrics.ASSESSMENT_TABLE_DELETE_ACTION)
      : access[2];
    if (deleteAccess) {
      actions.push({
        type: "delete",
        id: props.id,
        onClickEvent: onRemoveHandler
      });
    }
    return <TableRowActions actions={actions} />;
  }, []);

  const onSelectChange = useCallback((rowKeys: any, selectedRows: any) => {
    setSelectedRows(rowKeys);
    onSelectRows(map(selectedRows, row => row.id));
  }, []);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedRows,
      onChange: onSelectChange,
      hideDefaultSelections: false
    }),
    [selectedRows]
  );

  const mappedColumns = useMemo(
    () =>
      map(tableConfig(!!isCompact), column => {
        if (column.key === "id") {
          return {
            ...column,
            width: "10%",
            render: (item: any, record: any) => buildActions(record)
          };
        }
        return column;
      }),
    []
  );

  const jsxHeaders: JSXHeadersType = useMemo(() => {
    const columns = mappedColumns;
    let jsxHeaders: JSXHeadersType = [];
    columns.forEach((col: any) => {
      if (isValidElement(col?.title) && !col?.hidden) {
        let jsxTitle = col?.titleForCSV;
        jsxHeaders.push({
          title: jsxTitle ? jsxTitle : capitalize(col?.dataIndex?.replace(/_/g, " ")),
          key: col?.dataIndex
        });
      }
    });
    return jsxHeaders;
  }, [mappedColumns]);

  const clearSelectedIds = () => {
    setSelectedRows([]);
    onSelectRows([]);
  };

  const queryParamsFromFilters = useCallback((filters: any, tab = "assessments") => {
    if (!filters) {
      return {
        tab
      };
    }

    const { questionnaire_template_ids, created_at, updated_at, completed, tag_ids, priority, assignee_user_ids } =
      filters;
    const created_st = created_at ? created_at.$gt : "";
    const created_end = created_at ? created_at.$lt : "";
    const updated_st = updated_at ? updated_at.$gt : "";
    const updated_end = updated_at ? updated_at.$lt : "";
    return {
      questionnaire_template_ids:
        questionnaire_template_ids && (questionnaire_template_ids || []).map((p: any) => p.key),
      created_at: created_st,
      updated_at: updated_st,
      updated_end,
      created_end,
      completed: completed !== undefined ? (completed ? "true" : "false") : undefined,
      tag_ids: tag_ids && (tag_ids || []).map((p: any) => p.key),
      priority,
      tab,
      assignee_user_ids
    };
  }, []);

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const {
        created_at,
        created_end,
        updated_at,
        updated_end,
        completed,
        tag_ids,
        priority,
        questionnaire_template_ids
      } = filters;

      if (questionnaire_template_ids && questionnaire_template_ids.length > 0) {
        filters["questionnaire_template_ids"] = questionnaire_template_ids.map((id: any) => ({ key: id }));
      }

      if (completed && completed.length) {
        filters["completed"] = completed[0] === "true";
      }
      if (tag_ids && tag_ids.length) {
        filters["tag_ids"] = tag_ids.map((id: any) => ({ key: id }));
      }
      if (priority && priority.length) {
        filters["priority"] = priority[0];
      }
      if (created_at && created_end) {
        delete filters.created_end;
        filters["created_at"] = {
          $gt: created_at[0],
          $lt: created_end[0]
        };
      }
      if (updated_at && updated_end) {
        delete filters.updated_end;
        filters["updated_at"] = {
          $gt: updated_at[0],
          $lt: updated_end[0]
        };
      }
    }
    return filters;
  }, []);

  if (deleteLoading) return <Spin className="centered" />;

  return (
    <RestApiPaginatedTableUrlWrapper
      uri={"quiz"}
      title={"Assessments"}
      method="list"
      rowSelection={!isCompact && rowSelection}
      displayCount={false}
      derive={true}
      shouldDerive="tags"
      filters={props.moreFilters || {}}
      columns={mappedColumns}
      hasSearch={false}
      hasFilters={true}
      rowKey={"id"}
      buildQueryParamsFromFilters={queryParamsFromFilters}
      query_params_to_parse={queryParamsToParse}
      onQueryParamsParsed={handleParsedQueryParams}
      clearSelectedIds={clearSelectedIds}
      downloadCSV={{
        tableDataTransformer: levelopsAssessmentCountReportCSVTransformer,
        type: CSVDownloadSagaType.GENERIC_CSV_DOWNLOAD,
        jsxHeaders
      }}
    />
  );
};

export default AssessmentTable;
