import { notification } from "antd";
import { PluginResultsDiff } from "configurations/containers/plugin-results";
import { ReportPrint } from "configurations/containers/reports";
import { forEach, get } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { useReactToPrint } from "react-to-print";
import {
  pluginResultsBulkDelete,
  pluginResultsDiff,
  pluginResultsGet
} from "reduxConfigs/actions/restapi/pluginresultsActions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import {
  getGenericMethodSelector,
  getGenericRestAPISelector,
  useParamSelector
} from "reduxConfigs/selectors/genericRestApiSelector";
import { AntButton, AntRow } from "shared-resources/components";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { appendErrorMessagesHelper } from "utils/arrayUtils";

import { tableColumns } from "./table-config";

const queryParamsToParse = ["tag_ids", "product_ids", "ids"];

const PluginResultsListPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [selectedRowIds, setSelectedRowIds] = useState<any[]>([]);
  const [diffLoading, setDiffLoading] = useState<boolean>(false);
  const [pluginResultsLoading, setPluginResultsLoading] = useState<boolean>(false);
  const [reports, setReports] = useState<any[]>([]);
  const [showDiff, setShowDiff] = useState<boolean>(false);
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState<number>(1);

  const printRef = useRef(null);
  const dispatch = useDispatch();
  const pluginResultsDiffState = useParamSelector(getGenericRestAPISelector, {
    uri: "plugin_results",
    method: "diff",
    uuid: "0"
  });

  const pluginResultsGetState = useParamSelector(getGenericMethodSelector, {
    uri: "plugin_results",
    method: "get"
  });

  const pluginResultsBulkDeleteState = useParamSelector(getGenericRestAPISelector, {
    uri: "plugin_results",
    method: "bulkDelete",
    uuid: "0"
  });

  const handlePrint = useReactToPrint({ content: () => printRef.current, copyStyles: true });

  useEffect(() => {
    return () => {
      dispatch(restapiClear("plugin_results", "list", "0"));
      dispatch(restapiClear("plugin_results", "diff", "-1"));
      dispatch(restapiClear("plugin_results", "get", "-1"));
      dispatch(restapiClear("tags", "bulk", "0"));
      dispatch(restapiClear("plugin_results", "bulkDelete", "-1"));
    };
  }, []);

  useEffect(() => {
    if (diffLoading) {
      const loading = get(pluginResultsDiffState, ["loading"], true);
      const error = get(pluginResultsDiffState, ["error"], false);
      if (!loading && !error) {
        setDiffLoading(false);
      }
    }
  }, [pluginResultsDiffState]);

  useEffect(() => {
    if (bulkDeleting) {
      const loading = get(pluginResultsBulkDeleteState, ["loading"], true);
      const error = get(pluginResultsBulkDeleteState, ["error"], false);
      if (!loading) {
        if (!error) {
          const data = get(pluginResultsBulkDeleteState, ["data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(num => num + 1);
          } else {
            setSelectedRows([]);
            setSelectedRowIds([]);
            setReload(num => num + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [pluginResultsBulkDeleteState]);

  useEffect(() => {
    if (pluginResultsLoading) {
      let loading = false;
      let reports: any = [];
      forEach(selectedRowIds, (row: any) => {
        const curLoading = get(pluginResultsGetState, [row.id, "loading"], true);
        const curError = get(pluginResultsGetState, [row.id, "error"], false);
        if (!curLoading && !curError) {
          const rep = get(pluginResultsGetState, [row.id, "data"], false);
          rep.products = row?.products || {};
          reports.push(rep);
        } else {
          loading = true;
        }
      });
      setPluginResultsLoading(loading);
      setReports(reports);
    }
  }, [pluginResultsGetState]);

  const onSelectChange = useCallback((rowKeys: any, selectedRows: any) => {
    setSelectedRows(rowKeys);
    setSelectedRowIds(selectedRows);
    setPluginResultsLoading(true);
    selectedRows.forEach((row: any) => dispatch(pluginResultsGet(row?.id)));
  }, []);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedRows,
      onChange: onSelectChange
    }),
    [selectedRows]
  );

  const queryParamsFromFilters = useCallback((filters: any, tab = "plugins") => {
    if (!filters) {
      return {
        tab
      };
    }
    const { tag_ids, product_ids, ids } = filters;
    return {
      tab,
      tag_ids: tag_ids && (tag_ids || []).map((p: any) => p.key),
      product_ids: product_ids && (product_ids || []).map((p: any) => p.key),
      ids: ids && (ids || []).map((p: any) => p.key)
    };
  }, []);

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const { tag_ids, product_ids, ids } = filters;
      if (tag_ids && tag_ids.length) {
        filters["tag_ids"] = tag_ids.map((id: any) => ({ key: id }));
      }
      if (product_ids && product_ids.length) {
        filters["product_ids"] = product_ids.map((id: any) => ({ key: id }));
      }
      if (ids && ids.length) {
        filters["ids"] = ids.map((id: any) => ({ key: id }));
      }
    }
    return filters;
  }, []);

  const clearSelectedIds = useCallback(() => setSelectedRows([]), []);

  const onBulkDelete = useCallback(() => {
    dispatch(pluginResultsBulkDelete(selectedRows));
    setBulkDeleting(true);
  }, [selectedRows]);

  const diff = diffLoading === false && showDiff ? get(pluginResultsDiffState, ["data"], {}) : undefined;

  const tableStyle = useMemo(() => ({ minHeight: "600px" }), []);

  return (
    <>
      <AntRow type={"flex"} justify={"end"}>
        <AntButton
          icon={"file-search"}
          type="primary"
          className="mb-10"
          disabled={selectedRows.length !== 2}
          onClick={(e: any) => {
            if (selectedRows[0].tool !== selectedRows[1].tool) {
              notification.error({
                message: "Cannot DIFF",
                description: "Please select same tool results for DIFF"
              });
            } else {
              setDiffLoading(true);
              setShowDiff(true);
              dispatch(pluginResultsDiff(...selectedRowIds.map(row => row.id)));
            }
          }}>
          Diff Results
        </AntButton>
        <AntButton
          icon={"printer"}
          type={"primary"}
          className="ml-10 mb-10"
          onClick={handlePrint}
          disabled={selectedRows.length === 0 || pluginResultsLoading}>
          Print Report
        </AntButton>
      </AntRow>
      {!pluginResultsLoading && selectedRows.length > 0 && (
        //@ts-ignore
        <ReportPrint reports={[...reports]} ref={printRef} />
      )}
      <div style={tableStyle}>
        <RestApiPaginatedTableUrlWrapper
          pageName={"plugin_results"}
          uri="plugin_results"
          method="list"
          columns={tableColumns}
          rowSelection={rowSelection}
          hasFilters={true}
          hasSearch={false}
          generalSearchField="tool"
          rowKey={"id"}
          buildQueryParamsFromFilters={queryParamsFromFilters}
          query_params_to_parse={queryParamsToParse}
          onQueryParamsParsed={handleParsedQueryParams}
          onBulkDelete={onBulkDelete}
          hasDelete={true}
          clearSelectedIds={clearSelectedIds}
          bulkDeleting={bulkDeleting}
          reload={reload}
        />
        {diffLoading === false && showDiff && (
          <PluginResultsDiff
            diff={diff}
            visible={showDiff}
            onClose={() => {
              setShowDiff(false);
            }}
            rows={selectedRowIds}
          />
        )}
      </div>
    </>
  );
};

export default PluginResultsListPage;
