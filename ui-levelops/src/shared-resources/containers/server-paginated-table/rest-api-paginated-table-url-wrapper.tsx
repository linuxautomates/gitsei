import React, { useCallback, useEffect, useMemo, useState } from "react";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { useSelector } from "react-redux";
import { isEqual } from "lodash";

import { DEFAULT_PAGE_SIZE } from "constants/pageSettings";
import RestApiPaginatedTable, { RestApiPaginatedTableProps } from "./rest-api-paginated-table";
import { buildQueryParamsFromObject, parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";

interface RestApiPaginatedTableWrapperProps extends RestApiPaginatedTableProps {
  buildQueryParamsFromFilters?: (filters: any) => {};
  onQueryParamsParsed?: (parsedKeys: {}) => {};
  query_params_to_parse?: string[];
  savingFilters?: boolean;
  handleFilterSave?: () => void;
  disableSaveFilterButton?: boolean;
  updateInitialFilters?: boolean;
  setUpdateInitialFilters?: (value: boolean) => void;
  expandIconColumnIndex?: number;
  expandedRowKeys?: string[];
}

const RestApiPaginatedTableUrlWrapper: React.FC<RestApiPaginatedTableWrapperProps & RouteComponentProps> = (
  props: RestApiPaginatedTableWrapperProps & RouteComponentProps
) => {
  const SEARCH_QUERY_KEY = "name";
  const PAGE_QUERY_KEY = "page";

  const [pageSize, setPageSize] = useState(props.pageSize || DEFAULT_PAGE_SIZE);
  const pageSettingsState = useSelector(getPageSettingsSelector);
  const [showFiltersDropDown, setShowFiltersDropDown] = useState<boolean>(false);
  const [initialFilter, setInitialFilter] = useState<{ [key: string]: string | string[] | any } | undefined>(
    props.onQueryParamsParsed &&
      props.onQueryParamsParsed(parseQueryParamsIntoKeys(props.location.search, props.query_params_to_parse))
  );
  const [saveButtonEnable, setSaveButtonEnable] = useState<boolean>(false);

  const pageSettings = pageSettingsState ? pageSettingsState[props.pageName || "default"] : undefined;

  useEffect(() => {
    if (pageSettings && pageSettings.page_size !== pageSize) {
      setPageSize(pageSettings.page_size || DEFAULT_PAGE_SIZE);
    }
  }, [pageSettingsState]);

  const filters = useMemo(() => {
    return (
      props.onQueryParamsParsed &&
      props.onQueryParamsParsed(parseQueryParamsIntoKeys(props.location.search, props.query_params_to_parse))
    );
  }, [props.location.search]);

  useEffect(() => {
    if (!isEqual(initialFilter, filters)) {
      setSaveButtonEnable(!props.disableSaveFilterButton);
    } else {
      setSaveButtonEnable(false);
    }
  }, [filters, initialFilter, props.disableSaveFilterButton]);

  const currentValueForKeyInUrl = (key: string) => {
    const data = parseQueryParamsIntoKeys(props.location.search, [key]);
    return data[key] && data[key][0];
  };

  const page = useMemo(() => {
    return currentValueForKeyInUrl(PAGE_QUERY_KEY);
  }, [props.location.search]);

  const searchQuery = useMemo(() => {
    return currentValueForKeyInUrl(SEARCH_QUERY_KEY);
  }, [props.location.search]);

  const updateURLParams = (params: any) => {
    props.history.push({
      search: "?" + params
    });
  };

  const parsedFilterParams = (filters: any) => {
    let parsedParams: any = props.buildQueryParamsFromFilters && props.buildQueryParamsFromFilters(filters);
    if (!parsedParams) {
      parsedParams = {};
    }
    return parsedParams;
  };

  const handleFiltersChange = useCallback(
    (newFilters: any) => {
      if (!isEqual(filters, newFilters)) {
        const parsedParams = parsedFilterParams(newFilters);
        parsedParams[PAGE_QUERY_KEY] = 1;
        parsedParams[SEARCH_QUERY_KEY] = searchQuery;
        updateURLParams(buildQueryParamsFromObject(parsedParams));
      }
    },
    [props.location.search]
  );

  const handleSearchChange = useCallback(
    (searchQuery: string) => {
      const parsedParams = parsedFilterParams(filters);
      parsedParams[SEARCH_QUERY_KEY] = searchQuery;
      parsedParams[PAGE_QUERY_KEY] = 1;
      updateURLParams(buildQueryParamsFromObject(parsedParams));
    },
    [props.location.search]
  );

  const handlePageChange = useCallback(
    (page: number) => {
      const parsedParams = parsedFilterParams(filters);
      parsedParams[PAGE_QUERY_KEY] = page;
      parsedParams[SEARCH_QUERY_KEY] = searchQuery;
      updateURLParams(buildQueryParamsFromObject(parsedParams));
    },
    [props.location.search]
  );

  const handlePageSizeChange = useCallback(
    (pageSize: number) => {
      const parsedParams = parsedFilterParams(filters);
      parsedParams[SEARCH_QUERY_KEY] = searchQuery;
      parsedParams[PAGE_QUERY_KEY] = 1;
      updateURLParams(buildQueryParamsFromObject(parsedParams));
      setPageSize(pageSize);
    },
    [props.location.search]
  );

  const handleClearFilters = useCallback(() => {
    const filters = props.buildQueryParamsFromFilters ? props.buildQueryParamsFromFilters(undefined) : {};
    updateURLParams(buildQueryParamsFromObject(filters));
  }, []);

  const handleFilterSave = () => {
    props.handleFilterSave && props.handleFilterSave();
  };

  useEffect(() => {
    if (props.updateInitialFilters) {
      // updating initial filters
      setInitialFilter(filters);
      props.setUpdateInitialFilters && props.setUpdateInitialFilters(false);
    }
  }, [props.updateInitialFilters, filters]);

  const { title, pageName, uri, method, uuid, loading, hasFilters, bordered, hasSearch, scroll, reload } = props;

  return (
    <RestApiPaginatedTable
      pageName={pageName}
      title={title}
      uri={uri}
      method={method}
      columns={props.columns}
      uuid={uuid}
      page={page}
      hasFilters={hasFilters}
      bordered={bordered}
      hasSearch={hasSearch}
      searchQuery={searchQuery}
      scroll={scroll}
      pageSize={pageSize}
      onPageSizeChange={handlePageSizeChange}
      onPageChange={handlePageChange}
      onFiltersChange={handleFiltersChange}
      onSearchChange={handleSearchChange}
      clearFilters={handleClearFilters}
      filters={filters}
      loading={loading}
      hasDynamicColumns
      renderDynamicColumns={props.renderDynamicColumns}
      configureDynamicColumns={props.configureDynamicColumns}
      transformRecords={props.transformRecords}
      expandedRowRender={props.expandedRowRender}
      reload={reload}
      generalSearchField={props.generalSearchField}
      rowSelection={props.rowSelection}
      clearSelectedIds={props.clearSelectedIds}
      hasDelete={props.hasDelete}
      downloadCSV={props.downloadCSV}
      derive={props.derive}
      shouldDerive={props.shouldDerive}
      onBulkDelete={props.onBulkDelete}
      bulkDeleting={props.bulkDeleting}
      showFiltersDropDown={showFiltersDropDown}
      setShowFiltersDropDown={setShowFiltersDropDown}
      filterSaveButtonEnabled={saveButtonEnable}
      handleFilterSave={handleFilterSave}
      savingFilters={props.savingFilters}
      componentTitle={props.componentTitle}
      disableSaveFilterButton={props.disableSaveFilterButton}
      expandIconColumnIndex={props.expandIconColumnIndex}
      expandedRowKeys={props.expandedRowKeys}
      useRestTableAddFilters={props.useRestTableAddFilters}
      restTableFilterConfigs={props.restTableFilterConfigs}
    />
  );
};

export default withRouter(RestApiPaginatedTableUrlWrapper);
