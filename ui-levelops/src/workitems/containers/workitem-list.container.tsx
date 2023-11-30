import { getWorkitemDetailPage } from "constants/routePaths";
import { workItemCSVTransformer } from "dashboard/helpers/csv-transformers/workitem-csv.transformer";
import { CSVDownloadSagaType } from "dashboard/helpers/helper";
import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { restapiClear, restapiData, workItemBulkDelete } from "reduxConfigs/actions/restapi";
import { AntText } from "shared-resources/components";
import RestApiPaginatedTableUrlWrapper from "shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import {
  handleParsedQueryParams,
  queryParamsFromFilters,
  queryParamsToParse
} from "workitems/helpers/workitem-parsed-query.handler";
import { tableColumns } from "./table-config";
import { Link } from "react-router-dom";

interface WorkitemsListContainerProps {
  moreFilters: any;
  partialFilters?: any;
  hasSearch?: boolean;
  hasFilters?: boolean;
  title?: string;
  pageName?: string;
  compact?: boolean;
  uuid?: string;
  tab?: string;
  selectedIds?: any;
  rowSelection?: any;
  reload?: any;
  clearSelectedIds?: any;
  onBulkDelete?: any;
  bulkDeleting?: boolean;
  componentTitle?: any;
  savingFilters?: boolean;
  handleFilterSave?: () => void;
  disableSaveFilterButton?: boolean;
  updateInitialFilters?: boolean;
  setUpdateInitialFilters?: (value: boolean) => void;
}

const WorkitemsListContainer: React.FC<WorkitemsListContainerProps> = props => {
  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(restapiClear("workitem", "list", "0"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const compactColumns = ["vanity_id", "assignees", "status"];

  const columnsWithActions = props.compact
    ? tableColumns().filter(column => compactColumns.includes(column.key))
    : [...tableColumns()];

  const mappedColumns = columnsWithActions.map(column => {
    if (column.key === "vanity_id") {
      return {
        ...column,
        render: (item: any, record: any, index: number) => {
          return (
            <AntText className={"pl-10"}>
              <Link
                className={"ellipsis"}
                to={`${getWorkitemDetailPage()}?workitem=${record.vanity_id}`}
                // @ts-ignore
                onClick={e => dispatch(restapiData(record, "workitem", "get", `list/vanity-id/${record.vanity_id}`))}>
                {item}
              </Link>
            </AntText>
          );
        }
      };
    }
    return column;
  });
  return (
    <RestApiPaginatedTableUrlWrapper
      {...props}
      pageName={props.pageName}
      generalSearchField="title"
      derive={true}
      shouldDerive="tag_ids"
      uri="workitem"
      method="list"
      columns={mappedColumns}
      downloadCSV={{
        tableDataTransformer: workItemCSVTransformer,
        jsxHeaders: [{ title: "Name", key: "vanity_id" }],
        type: CSVDownloadSagaType.GENERIC_CSV_DOWNLOAD
      }}
      buildQueryParamsFromFilters={filters => queryParamsFromFilters(filters, props.tab || "all", props.moreFilters)}
      query_params_to_parse={queryParamsToParse}
      onQueryParamsParsed={handleParsedQueryParams}
      hasDelete={true}
    />
  );
};

export default WorkitemsListContainer;
