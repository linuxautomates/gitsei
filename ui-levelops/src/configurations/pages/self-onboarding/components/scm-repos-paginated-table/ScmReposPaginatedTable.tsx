import { Card, Spin } from "antd";
import { ColumnProps } from "antd/lib/table";
import { debounce, get, map } from "lodash";
import React, { ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntInput, AntTable, AntText } from "shared-resources/components";
import { RestApiPaginatedTableProps } from "shared-resources/containers/server-paginated-table/rest-api-paginated-table";
import { sanitizeObject } from "utils/commonUtils";
import { NO_REPO_FOUND_TEXT } from "../../constants";
import { ScmReposConfigType, SCMReposDataType } from "../../types/integration-step-components-types";

interface ScmReposPaginatedTableProps extends RestApiPaginatedTableProps {
  hasTitleSearch?: boolean;
  searchPlaceholder?: string;
  searchClassName?: string;
  className?: string;
  customExtraContent?: ReactNode;
  searchURI?: string;
  moreFilters?: any;
  selectedRepos?: Array<SCMReposDataType>;
  showSelectedRepos: boolean;
  setShowSelectedRepos: (value: any) => void;
}
const ScmReposPaginatedTable: React.FC<ScmReposPaginatedTableProps> = (props: ScmReposPaginatedTableProps) => {
  const {
    showTitle,
    title,
    hasTitleSearch,
    searchPlaceholder,
    searchClassName,
    className,
    customExtraContent,
    uri,
    uuid,
    method,
    searchURI,
    columns,
    rowKey,
    rowSelection,
    moreFilters,
    generalSearchField,
    selectedRepos,
    showSelectedRepos,
    setShowSelectedRepos
  } = props;
  const [searchField, setSearchField] = useState<string>("");
  const [scmReposLoading, setScmReposLoading] = useState<boolean>(false);
  const [scmReposConfig, setScmReposConfig] = useState<ScmReposConfigType>({
    records: [],
    _metadata: { total_count: 0 }
  });

  const [scmReposPageConfig, setScmReposPageConfig] = useState<{ page: number; page_size: number }>({
    page: 1,
    page_size: 10
  });
  const reload = useRef<number>(1);
  const prevPageRef = useRef<number>(-1);

  const dispatch = useDispatch();
  const finalURI = useMemo(() => {
    return searchField && !showSelectedRepos ? searchURI : uri;
  }, [searchField, uri, searchURI, showSelectedRepos]);

  const scmReposRecordsState = useParamSelector(getGenericRestAPISelector, {
    uri: finalURI,
    uuid,
    method
  });

  const getScmReposFilters = () => {
    let filters = {
      page: scmReposPageConfig.page - 1,
      page_size: scmReposPageConfig.page_size,
      filter: {
        ...(moreFilters ?? {})
      }
    };
    if (searchField) {
      filters = {
        ...filters,
        filter: { ...get(filters, ["filter"], {}), [generalSearchField ?? "name"]: searchField }
      };
    }
    return sanitizeObject(filters);
  };

  const fetchSCMRepos = () => {
    if (!showSelectedRepos) {
      reload.current = reload.current + 1;
      setScmReposLoading(true);
      dispatch(genericList(finalURI, method, getScmReposFilters(), null, uuid));
    }
  };

  const debouncedFetchRepos = debounce((search: string) => {
    if (!showSelectedRepos) {
      setScmReposLoading(true);
      let payload = getScmReposFilters();
      const newUri = search ? searchURI : uri;
      payload = { ...payload, filter: { ...(payload.filter || {}), [generalSearchField ?? "name"]: search } };
      dispatch(genericList(newUri, method, payload, null, uuid));
    }
    if (!search) {
      onPageChangeHandler(1);
    }
  }, 1000);

  const debouncedFetchReposRef = useRef(debouncedFetchRepos);

  const getDataSource = useMemo(() => {
    if (searchField && !scmReposLoading && !scmReposConfig.records.length) {
      return [{ name: "", url: NO_REPO_FOUND_TEXT, updated_at: 0, description: "" }];
    }
    return scmReposLoading ? [] : scmReposConfig.records;
  }, [selectedRepos, scmReposLoading, scmReposConfig, showSelectedRepos, scmReposPageConfig, searchField]);

  useEffect(() => {
    if (!scmReposLoading && reload.current !== 1) {
      setScmReposLoading(true);
      let payload = getScmReposFilters();
      if (showSelectedRepos) {
        payload = {
          ...payload,
          page: 0,
          page_size: selectedRepos?.length
        };
      }
      dispatch(genericList(uri, method, payload, null, uuid));
    }
  }, [showSelectedRepos]);

  useEffect(() => {
    fetchSCMRepos();
  }, [scmReposPageConfig.page]);

  useEffect(() => {
    if (scmReposLoading) {
      const loading = get(scmReposRecordsState, ["loading"], true);
      const error = get(scmReposRecordsState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(scmReposRecordsState, ["data"], {});
          setScmReposConfig(data);
        }
        setScmReposLoading(false);
      }
    }
  }, [scmReposLoading, scmReposRecordsState]);

  const handleSearchChange = (e: any) => {
    const newVal = e.target.value;
    setSearchField(newVal);
    debouncedFetchReposRef.current(newVal);
  };

  const handleShowSelectedRepos = () => {
    setShowSelectedRepos(!showSelectedRepos);
  };

  const onPageChangeHandler = (page: number) => {
    setScmReposPageConfig(prev => ({
      ...prev,
      page: page
    }));
  };

  const showCustomChanger = useMemo(
    () => !scmReposLoading && (scmReposConfig._metadata.total_count ?? 0) < scmReposPageConfig.page_size,
    [scmReposLoading, scmReposConfig, scmReposPageConfig]
  );

  const renderTitle = useMemo(
    () => ({
      title: (
        <div className="flex align-center flex-wrap">
          <div className="flex direction-column align-items-start">
            <div className="flex">
              <AntText style={{ marginRight: ".3rem" }}>{showTitle && title} </AntText>
              {hasTitleSearch && (
                <AntInput
                  id={`scm-repos-search`}
                  placeholder={searchPlaceholder}
                  type="search"
                  onChange={handleSearchChange}
                  name="general-search"
                  className={searchClassName || ""}
                  value={searchField}
                />
              )}
            </div>
          </div>
        </div>
      )
    }),
    [showTitle, title, searchField, searchPlaceholder, searchClassName, hasTitleSearch, handleSearchChange]
  );

  const mappedColumns: Array<ColumnProps<any>> = useMemo(
    () =>
      map(columns ?? [], (column: ColumnProps<any>) => {
        if (column.dataIndex === "name") {
          return {
            ...column,
            title: (
              <AntText>
                REPO (
                <span className="select-count-link" onClick={handleShowSelectedRepos}>
                  {selectedRepos?.length} selected
                </span>
                )
              </AntText>
            )
          };
        }
        return column;
      }),
    [columns, selectedRepos, scmReposConfig]
  );

  const renderLocale = useMemo(() => {
    return {
      emptyText: scmReposLoading ? (
        <div className="flex justify-center align-center">
          <Spin />
        </div>
      ) : (
        ""
      )
    };
  }, [scmReposLoading]);

  return (
    <Card bordered={false} {...renderTitle} extra={customExtraContent}>
      <div>
        <AntTable
          hasCustomPagination={!scmReposLoading}
          dataSource={getDataSource}
          columns={mappedColumns}
          onPageChange={onPageChangeHandler}
          pageSize={showSelectedRepos ? selectedRepos?.length : scmReposPageConfig.page_size}
          page={showSelectedRepos ? 0 : scmReposPageConfig.page}
          className={className}
          showPageSizeOptions={false}
          locale={renderLocale}
          pagination={false}
          rowSelection={rowSelection}
          totalRecords={showSelectedRepos ? selectedRepos?.length : scmReposConfig._metadata.total_count ?? 0}
          size={"middle"}
          rowKey={rowKey}
          showCustomChanger={showCustomChanger}
        />
      </div>
    </Card>
  );
};

export default ScmReposPaginatedTable;
