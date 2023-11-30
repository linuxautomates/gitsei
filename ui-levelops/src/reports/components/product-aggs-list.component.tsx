import { forEach, get } from "lodash";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { useReactToPrint } from "react-to-print";
import { productAggsGet, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericMethodSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { SnykPrintReportComponent } from "./snyk-print-report.component";
import { AntButton, AntRow, TableRowActions } from "shared-resources/components";
import { tableConfig } from "./table-config";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { getBaseUrl } from "constants/routePaths";

const queryParamsToParse = ["product_id", "integration_ids"];

const ProductAggsListComponent: React.FC = () => {
  const [selectedRows, setSelectedRows] = useState<any>([]);
  const [selectedRowIds, setSelectedRowIds] = useState<any>([]);
  const [integrationResultsLoading, setIntegrationResultsLoading] = useState<boolean>(false);
  const [reports, setReports] = useState<any>([]);

  const printRef = useRef(null);
  const dispatch = useDispatch();

  const productAggsState = useParamSelector(getGenericMethodSelector, { uri: "product_aggs", method: "get" });

  const handlePrint = useReactToPrint({
    content: () => printRef.current,
    copyStyles: true
  });

  useEffect(() => {
    return () => {
      dispatch(restapiClear("product_aggs", "list", "0"));
      dispatch(restapiClear("products", "bulk", "0"));
      dispatch(restapiClear("integrations", "bulk", "0"));
    };
  }, []);

  useEffect(() => {
    if (integrationResultsLoading) {
      let curloading: boolean = false;
      let reports: any = [];

      forEach(selectedRowIds, (row: any) => {
        const loading = get(productAggsState, [row?.id, "loading"], true);
        const error = get(productAggsState, [row?.id, "error", false]);

        if (!loading && !error) {
          const rep = get(productAggsState, [row?.id, "data"], {});
          rep.products = row?.workspaces || [];
          rep.integrations = row?.integrations || {};
          reports.push(rep);
        } else if (loading) {
          curloading = true;
        }
      });

      setIntegrationResultsLoading(curloading);
      setReports(reports);
    }
  }, [productAggsState]);

  const queryParamsFromFilters = useCallback((filters: any, tab = "integrations") => {
    if (!filters) {
      return {
        tab
      };
    }
    const { product_id, integration_ids } = filters;
    return {
      product_id,
      integration_ids: integration_ids && (integration_ids || []).map((p: any) => p.key),
      tab
    };
  }, []);

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const { product_id, integration_ids } = filters;
      if (product_id && product_id.length > 0) {
        filters["product_id"] = product_id[0];
      }
      if (integration_ids && integration_ids.length) {
        filters["integration_ids"] = integration_ids.map((id: any) => ({ key: id }));
      }
    }
    return filters;
  }, []);

  const onEditHandler = useCallback((qId: any) => {
    let url = `${getBaseUrl()}/reports/view-integration-report`.concat(`?report=${qId}`);
    window.location.href = url;
  }, []);

  const buildActionOptions = useMemo(
    () => (props: any) => {
      const actions = [
        {
          type: "eye",
          id: props?.id,
          onClickEvent: onEditHandler
        }
      ];
      return <TableRowActions actions={actions} />;
    },
    []
  );

  const onSelectChange = useCallback((rowKeys: any, selectedRows: any) => {
    forEach(selectedRows, (row: any) => {
      dispatch(productAggsGet(row?.id));
    });

    setSelectedRows(rowKeys);
    setSelectedRowIds(selectedRows);
    setIntegrationResultsLoading(true);
  }, []);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedRows,
      onChange: onSelectChange,
      hideDefaultSelections: false
    }),
    [selectedRows]
  );

  const clearSelectedIds = useCallback(() => setSelectedRows([]), []);

  const mappedColumns = useMemo(() => {
    return tableConfig.map((column: any) => {
      if (column.key === "id") {
        return {
          ...column,
          render: (item: any, record: any) => buildActionOptions(record)
        };
      }
      return column;
    });
  }, []);

  return (
    <>
      <AntRow type={"flex"} justify={"end"}>
        <AntButton
          icon={"printer"}
          type={"primary"}
          className="ml-10 mb-10"
          onClick={handlePrint}
          disabled={selectedRows?.length === 0 || integrationResultsLoading}>
          Print Report
        </AntButton>
      </AntRow>
      {!integrationResultsLoading && selectedRows?.length > 0 && (
        <SnykPrintReportComponent ref={printRef} reports={reports} />
      )}
      <RestApiPaginatedTableUrlWrapper
        uri={"product_aggs"}
        rowSelection={rowSelection}
        displayCount={false}
        method="list"
        columns={mappedColumns}
        hasSearch={false}
        rowKey={"id"}
        buildQueryParamsFromFilters={queryParamsFromFilters}
        query_params_to_parse={queryParamsToParse}
        onQueryParamsParsed={handleParsedQueryParams}
        clearSelectedIds={clearSelectedIds}
      />
    </>
  );
};

export default ProductAggsListComponent;
