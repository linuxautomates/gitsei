import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { RunsLogs } from "../../components";
import { tableColumns } from "./table-config";
import { parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { RouteComponentProps } from "react-router-dom";
import { useDispatch } from "react-redux";
import { propelRunRerun, propelRunsBulkDelete } from "reduxConfigs/actions/restapi/propelRuns.actions";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { get } from "lodash";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";
import { TableRowActions } from "shared-resources/components";
import { getPropelRunReRunSelector } from "reduxConfigs/selectors/propel-run-rerun.selector";
import { useHasEntitlements } from "./../../../custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "./../../../custom-hooks/constants";

interface RunsListContainerProps extends RouteComponentProps {}

const RunsListContainer: React.FC<RunsListContainerProps> = (props: RunsListContainerProps) => {
  const [moreFilters, setMoreFilters] = useState<any>({});
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [rerunning, setReRunning] = useState<boolean>(false);
  const [reload, setReload] = useState(1);
  const { location } = props;
  const dispatch = useDispatch();
  const entPropels = useHasEntitlements(Entitlement.PROPELS);

  const rerunId = useRef<string>();

  const propelRunsApiState = useParamSelector(getGenericUUIDSelector, {
    uri: "propel_runs",
    method: "bulkDelete",
    uuid: "0"
  });

  const propelRunRerunGetState = useParamSelector(getPropelRunReRunSelector, { id: rerunId.current });

  useEffect(() => {
    const queryFilters = parseQueryParamsIntoKeys(location.search, ["runbook_id", "state"]);
    if (queryFilters) {
      const { runbook_id, state } = queryFilters;
      if (runbook_id) {
        queryFilters["runbook_id"] = runbook_id[0];
      }
      if (state) {
        queryFilters["state"] = state[0];
      }
      setMoreFilters(queryFilters);
    }

    return () => {
      dispatch(restapiClear("propel_runs", "bulkDelete", "-1"));
    };
  }, []); // eslint-disable-next-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = propelRunsApiState;
      if (!loading) {
        if (!error) {
          const data = get(propelRunsApiState, ["data", "records"], []);
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
  }, [propelRunsApiState]); // eslint-disable-next-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (rerunning) {
      const { loading, error } = propelRunRerunGetState;
      if (!loading) {
        if (!error) {
          setReload(reload => reload + 1);
          notification.success({ message: "Runbook Rerun successfull" });
        } else {
          notification.error({ message: "Runbook Rerun failed" });
        }
        setReRunning(false);
        rerunId.current = undefined;
      }
    }
  }, [propelRunRerunGetState]); // eslint-disable-next-line react-hooks/exhaustive-deps

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const { runbook_id, state } = filters;
      if (runbook_id && runbook_id.length) {
        filters["runbook_id"] = runbook_id[0];
      }

      if (state && state.length) {
        filters["state"] = state[0];
      }
    }
    return filters;
  }, []);

  const queryParamsFromFilters = useCallback((filters: any, tab = "runs") => {
    if (!filters) {
      return {
        tab
      };
    }
    return {
      ...filters,
      tab
    };
  }, []);

  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const onBulkDelete = () => {
    dispatch(propelRunsBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange,
      getCheckboxProps: (rec: any) => ({
        disabled: !entPropels
      })
    }),
    [selectedIds, entPropels]
  );

  const rerun = useCallback((id: string, description: string) => {
    dispatch(propelRunRerun(id));
    rerunId.current = id;
    setReRunning(true);
    notification.info({ message: `${description} Runbook` });
  }, []);

  const buildActionOptions = (record: any) => {
    const desc =
      record?.state === "failure"
        ? "Retry"
        : record?.state === "success"
        ? !entPropels
          ? TOOLTIP_ACTION_NOT_ALLOWED
          : "Rerun"
        : "Running";
    const actions = [
      {
        type: "redo",
        id: record.id,
        description: desc,
        disabled: record?.state === "running" || !entPropels,
        toolTip: !entPropels ? TOOLTIP_ACTION_NOT_ALLOWED : "",
        onClickEvent: (id: string) => rerun(id, desc)
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const mappedColumns = useMemo(
    () =>
      tableColumns.map(column => {
        if (column.dataIndex === "id") {
          return {
            ...column,
            fixed: undefined,
            width: 100,
            render: (text: string, record: any, index: number) => buildActionOptions(record)
          };
        }
        return column;
      }),
    [entPropels]
  );

  return (
    <RestApiPaginatedTableUrlWrapper
      title={"Runs"}
      pageName={"propel_runs"}
      uri={"propel_runs"}
      method="list"
      columns={mappedColumns}
      hasFilters
      hasSearch={false}
      filters={moreFilters}
      buildQueryParamsFromFilters={queryParamsFromFilters}
      query_params_to_parse={["runbook_id", "state"]}
      onQueryParamsParsed={handleParsedQueryParams}
      expandedRowRender={(record: any) => <RunsLogs propel={record} />}
      hasDelete={true}
      clearSelectedIds={clearSelectedIds}
      rowSelection={rowSelection}
      onBulkDelete={onBulkDelete}
      reload={reload}
      bulkDeleting={bulkDeleting}
    />
  );
};

export default RunsListContainer;
