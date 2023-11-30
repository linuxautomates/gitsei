import "./runs-logs.scss";

import { Card, Spin } from "antd";
import { useGenericApi } from "custom-hooks";
import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { toTitleCase } from "utils/stringUtils";
import { getItem, nodeDetailTableConfig } from "workflow/components/runs-logs/node-detail.config";

import { AntButton, AntCol, AntInput, AntModal, AntRow, AntTable, AntText } from "../../../shared-resources/components";
import { PROPELS_ROUTES, getBaseUrl } from "../../../constants/routePaths";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { LogCard } from "workflow/components/runs-logs/components/LogCard";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { runsLogMappedColumns, runsLogStatusType } from "./helper";

const queryParamsToParse = ["runbookId", "runId", "runState", "propelId", "node_ids", "states"];

const RunsLogsList: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const {
    runId: runIds,
    propelId: propelIds,
    runbookId: runbookIds,
    runState: runStates
  } = parseQueryParamsIntoKeys(props.location.search, queryParamsToParse);

  const runbookId = runbookIds[0];
  const runState = runStates[0];
  const runId = runIds ? runIds[0] : null;
  const propelId = propelIds ? propelIds[0] : null;

  if (!runId || !propelId) {
    props.history.push(`${getBaseUrl()}${PROPELS_ROUTES._ROOT}?tab=runs`);
  }

  const dispatch = useDispatch();

  // @ts-ignore
  const [propelLoading, propelData] = useGenericApi({ id: propelId, uri: "propels", method: "get" });

  const [runLoading, runData] = useGenericApi({
    id: runId,
    uri: "propel_runs",
    method: "get"
  });

  const [page, setPage] = useState<any>(1);
  const [pageSize, setPageSize] = useState(50);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedRow, setSelectedRow] = useState<any>(undefined);
  const runsLogsData = useRef<any>(undefined);

  const propelRunsState = useParamSelector(getGenericRestAPISelector, {
    uuid: "0",
    method: "list",
    uri: "propel_runs_logs"
  });

  useEffect(() => {
    const loading = get(propelRunsState, ["loading"], true);
    const error = get(propelRunsState, ["error"], true);
    if (!loading && !error) {
      runsLogsData.current = get(propelRunsState, ["data", "records"], []);
    }
  }, [propelRunsState]);

  useEffect(() => {
    if (runsLogsData.current?.length && Object.keys(propelData).length) {
      dispatch(setPageSettings(props.location.pathname, { title: propelData?.name }));
    }
  }, [runsLogsData, propelData]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    return () => {
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const getNode = useCallback(
    (nodeId: string) => {
      const nodes = get(propelData, ["ui_data", "nodes"], {});
      const _node = Object.keys(nodes).find((n: any) => nodes[n]["id"] === nodeId);
      return _node ? nodes[_node] : {};
    },
    [propelData]
  );

  const getNodeName = useCallback(
    (nodeId: string) => {
      const node = getNode(nodeId);
      return node ? node.name : "";
    },
    [propelData]
  );

  const handlePageChange = useCallback(page => setPage(page), []);

  const handlePageSizeChange = useCallback(pageSize => setPageSize(pageSize), []);

  const handleSearchChange = useCallback(e => {
    setSearchQuery(e.target.value);
    setPage(1);
  }, []);

  const handleRowClick = useCallback(
    (index: number) => {
      return (e: any) => {
        setSelectedRow(tableData[index]);
      };
    },
    [searchQuery, runData]
  );

  const handleModalClose = useCallback(() => setSelectedRow(undefined), []);

  const tableData = useMemo(() => {
    let tableData: any[] = [];
    const filteredArgs = Object.keys(runData?.args || {}).filter(arg => {
      if (!searchQuery) {
        return true;
      }
      return arg.startsWith(searchQuery);
    });
    filteredArgs.forEach(arg => {
      tableData.push({ ...(runData?.args?.[arg] || {}) });
    });
    return tableData;
  }, [searchQuery, runData]);

  const filteredTableData = useMemo(() => {
    const pageNumber = page - 1;
    return tableData.slice(pageNumber * pageSize, pageNumber * pageSize + pageSize);
  }, [searchQuery, page, pageSize, runData]);

  const totalCount = useMemo(() => tableData.length, [searchQuery, runData]);

  const buttonStyle = useMemo(() => ({ padding: 0, fontSize: "14px" }), []);
  const cardBodyStyle = useMemo(() => ({ padding: "2px 0px 10px" }), []);

  const footer = useMemo(() => [], []);

  const totalPages = useMemo(
    () => (tableData ? Math.ceil(totalCount / pageSize) * 2 : 1),
    [runData, pageSize, searchQuery]
  );

  const mappedColumns = useMemo(
    () =>
      nodeDetailTableConfig.map(column => {
        if (column.dataIndex === "value") {
          return {
            ...column,
            render: (item: any, record: any, index: number) => {
              const value = getItem(item);
              return (
                <>
                  <AntText>{value && value.slice(0, 100)}</AntText>
                  {value.length > 100 && (
                    <AntButton type={"link"} style={buttonStyle} onClick={handleRowClick(index)}>
                      ...Read More
                    </AntButton>
                  )}
                </>
              );
            }
          };
        }
        return column;
      }),
    [searchQuery, runData]
  );

  const renderExtras = useMemo(
    () => (
      <AntRow gutter={[16]} align="bottom">
        <AntCol span={24}>
          <AntInput placeholder="Search ... " type="search" onChange={handleSearchChange} />
        </AntCol>
      </AntRow>
    ),
    []
  );

  const renderTable = useMemo(
    () => (
      <Card bordered={false} extra={renderExtras} title={"Arguments"} bodyStyle={cardBodyStyle}>
        <AntTable
          hasCustomPagination
          onPageChange={handlePageChange}
          onPageSizeChange={handlePageSizeChange}
          pageSize={pageSize}
          page={page}
          totalPages={totalPages}
          totalRecords={totalCount}
          dataSource={filteredTableData}
          columns={mappedColumns}
        />
      </Card>
    ),
    [page, pageSize, runData, searchQuery]
  );

  const renderValueModal = useMemo(() => {
    if (!selectedRow) {
      return null;
    }

    const value = getItem(selectedRow?.value);
    return (
      <AntModal
        visible={true}
        width={700}
        title={toTitleCase(selectedRow?.key || "")}
        centered
        onCancel={handleModalClose}
        footer={footer}>
        <span>{value}</span>
      </AntModal>
    );
  }, [selectedRow]);

  const queryParamsFromFilters = (filters: any) => {
    if (!filters) {
      return {
        runbookId,
        runId: runIds[0],
        runState,
        propelId: propelIds[0]
      };
    }
    const { run_id, propel_id, node_ids, states } = filters;
    return {
      runbookId,
      runId: run_id,
      runState,
      propelId: propel_id,
      node_ids,
      states
    };
  };

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const newFilters: any = {};
      const { propelId, runId, node_ids, states, runState } = filters;
      if (propelId && propelId.length > 0) {
        newFilters["propel_id"] = propelId[0];
      }
      if (runId && runId.length > 0) {
        newFilters["run_id"] = runId[0];
      }
      if (node_ids && node_ids.length > 0) {
        newFilters["node_ids"] = node_ids;
      }
      if (states && states.length > 0) {
        newFilters["states"] = states;
      } else if (runState[0] === runsLogStatusType.FAILURE) {
        newFilters["states"] = [runsLogStatusType.FAILURE];
      }
      return newFilters;
    }
  }, []);

  const renderRunsLogsTable = (
    <RestApiPaginatedTableUrlWrapper
      title="Node Run Logs"
      uri="propel_runs_logs"
      method="list"
      hasSearch={false}
      hasFilters
      columns={runsLogMappedColumns(get(propelData, ["ui_data", "nodes"], {}))}
      pageSize={100}
      expandedRowRender={(record: any) => <LogCard log={record} />}
      buildQueryParamsFromFilters={queryParamsFromFilters}
      query_params_to_parse={queryParamsToParse}
      onQueryParamsParsed={handleParsedQueryParams}
    />
  );

  if (propelLoading || runLoading) return <Spin className="h-100 w-100 centered" />;

  return (
    <div className="runs-logs-list">
      {renderTable}
      {renderRunsLogsTable}
      {renderValueModal}
    </div>
  );
};

export default RunsLogsList;
