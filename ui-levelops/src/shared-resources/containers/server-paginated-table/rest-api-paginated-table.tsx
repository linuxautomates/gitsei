import { Badge, Card, Spin, Tag } from "antd";
import cx from "classnames";
import { API_CALL_ON_TABLE_SORT_CHANGE, DEFAULT_PAGE_SIZE } from "constants/pageSettings";
import { CSVDownloadSagaType } from "dashboard/helpers/helper";
import ErrorWrapper from "hoc/errorWrapper";
import PaginatedTable, { PaginatedTableProps } from "./containers/table/paginated-table";
import { cloneDeep, get, map, orderBy, uniq } from "lodash";
import React, { isValidElement, useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { csvDownloadTriageGridView } from "reduxConfigs/actions/csvDownload.actions";
import { setPage } from "reduxConfigs/actions/pagesettings.actions";
import { paginationGet } from "reduxConfigs/actions/paginationActions";
import { genericTableCSVDownload } from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntButton, AntText } from "shared-resources/components";
import { defaultTriageGridViewColumns } from "triage/pages/grid-view/grid-table-config";

import TableFiltersContainer from "./containers/filters/table-filters.container";
import { buildServerPaginatedQuery } from "./helper";
import { PaginatedSelectPopup } from "./paginated-table-select-popup.component";
import { ExtraFilterContent } from "./extra-filter-content";
import { TRIAGE_UUID } from "../../../triage/pages/grid-view/grid-view-constant";

import "./rest-api-paginated-table.style.scss";
import NewTableFiltersContainer from "./containers/filters/new-table-filters.container";
import { getStartOfDayFromDateString, getEndOfDayFromDateString } from "../../../utils/dateUtils";
import { ISSUES_UUID } from "reduxConfigs/actions/restapi/issueFilter.actions";
import RestTableAddFilterContainer from "./containers/filters/rest-table-add-filter-container/RestTableAddFilterContainer";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export interface RestApiPaginatedTableProps extends PaginatedTableProps {
  pageName?: string;
  searchQuery?: string;
  filters?: any;
  uri: string;
  method: string;
  uuid?: string;
  derive?: boolean;
  shouldDerive?: any;
  reload?: number;
  hasFilters?: boolean;
  hasSort?: boolean;
  hasSearch?: boolean;
  generalSearchField?: string;
  clearFilters?: () => void;
  pagination_total?: number;
  title?: string;
  showTitle?: string;
  componentTitle?: any;
  expandedRowRender?: any;
  onRow?: any;
  rowSelection?: any;
  rowKey?: any;
  dataFilter?: any;
  sort?: any;
  across?: any;
  onFiltersChange?: any;
  onSearchChange?: any;
  hasDynamicColumns?: boolean;
  renderDynamicColumns?: any;
  configureDynamicColumns?: any;
  loading?: boolean;
  sort_order?: any;
  partial_parsed?: any;
  search_parsed?: any;
  onSortChange?: any;
  displayCount?: boolean;
  onPageSizeChange?: any;
  defaultExpandedRowKeys?: number;
  expandedRowKeys?: string[];
  downloadCSV?: {
    tableDataTransformer?: (data: any) => any; // This transformer is used to transform the apidata received from api-call to CSV supportable format
    jsxHeaders?: { title: string; key: string }[]; // jsxHeaders helps to set title for those columns which are valid React Element.
    type: CSVDownloadSagaType; // type helps to decide which saga is to be called
    onClickCSVDownload?: () => void; // For some cases we require to deal with csv download in parent component, this function is passed from parent to acheive the same.
  };
  hasDelete?: boolean;
  clearSelectedIds?: () => void;
  onBulkDelete?: () => void;
  bulkDeleting?: boolean;
  transformRecords?: (records: any[]) => any[];
  showFiltersDropDown?: boolean;
  setShowFiltersDropDown?: (val: boolean) => void;
  filterSaveButtonEnabled?: boolean;
  handleFilterSave?: () => void;
  savingFilters?: boolean;
  extraSuffixActionButtons?: React.ReactNode;
  useRestTableAddFilters?: boolean;
  restTableFilterConfigs?: LevelOpsFilter[];
  isDevRawStatsDrilldown?: boolean;
  fullName?: string;
  extraPropsForGraph?: any;
}

const RestApiPaginatedTable: React.FC<RestApiPaginatedTableProps> = (props: RestApiPaginatedTableProps) => {
  const dispatch = useDispatch();
  const {
    filters,
    uri,
    useRestTableAddFilters,
    restTableFilterConfigs,
    isDevRawStatsDrilldown,
    fullName,
    extraPropsForGraph
  } = props;

  const page = props.page ? Number(props.page) : 1;
  const method = props.method || "list";
  const hasFilters = props.hasFilters === false ? false : true;
  const hasDelete = props.hasDelete || false;
  const hasSort = props.hasSort || false;
  const hasSearch = props.hasSearch ?? true;
  const pageName = props.pageName || "default";
  const reload = props.reload || 1;
  const uuid = props.uuid || "0";
  const shouldDerive = props.shouldDerive || "all";
  const derive = props.derive ?? true;
  const scroll = props.scroll || {};
  const searchParsed = props.search_parsed || {};
  const generalSearchField = props.generalSearchField || "name";
  const showTitle = props.showTitle || true;
  const pageSize = props.pageSize || DEFAULT_PAGE_SIZE;
  const dataFilter = props.dataFilter || { field: "id", values: [] };
  const rowKey = props.rowKey || "id";
  const downloadCSV = props.downloadCSV || undefined;
  const extraSuffixActionButtons = props.extraSuffixActionButtons || undefined;

  const [totalRecords, setTotalRecords] = useState<number>(0);
  const [cardTitle, setCardTitle] = useState<any>([]);
  const [dataSource, setDataSource] = useState<any[]>([]);
  const [columns, setColumns] = useState<any[]>(props.columns);
  const [partialParsed, setPartialParsed] = useState<any>({});
  const [defaultExpandIndex, setDefaultExpandIndex] = useState(true);
  const [expanded_row_index, setExpandedRow] = useState<number | undefined>(undefined);
  const [showFilters, setShowFilters] = useState(false);
  const [loading, setLoading] = useState(true);
  const [sortOrder, setSortOrder] = useState<any>({});
  const [showWarningPopup, setShowWarningPopup] = useState(false);
  const [showDeletePopup, setShowDeletePopup] = useState(false);
  const [pendingAction, setPendingAction] = useState<any>({});
  const [bulkDeleteLoading, setBulkDeleteLoading] = useState<boolean>(false);

  const handleGeneralSearchChange = useCallback(
    (event: any | undefined) => {
      const selectionRowsCount = props.rowSelection?.selectedRowKeys?.length;
      if (selectionRowsCount > 0) {
        setShowWarningPopup(true);
        setPendingAction({
          type: "generalSearch",
          args: { value: event.target.value }
        });
      } else {
        generalSearchField && props.onSearchChange && props.onSearchChange(event.target.value);
      }
    },
    [filters, props.rowSelection, columns]
  );

  const cardBodyStyle = useMemo(() => ({ padding: "2px 0px 10px" }), []);

  const visibleColumns = useMemo(() => columns.filter((c: any) => !c.hidden), [columns]);

  const getCountForAppliedFilters = useMemo(() => {
    let totalKeys = [...Object.keys(filters || {}), ...Object.keys(get(filters, ["exclude"], {}))];
    const columnsWithPrefixPath = columns.filter((column: any) => column.prefixPath !== undefined);
    columnsWithPrefixPath.forEach((column: any) => {
      totalKeys = [
        ...totalKeys,
        ...Object.keys(get(filters, [column.prefixPath], {})),
        ...Object.keys(get(filters, ["exclude", column.prefixPath], {}))
      ];
    });

    const appliedFilters = uniq(totalKeys).filter(filter =>
      columns.map((column: any) => column.filterField || "nothing").includes(filter)
    );
    return appliedFilters ? appliedFilters.length : 0;
  }, [filters, columns]);

  const handleFiltersClick = useCallback(() => {
    setShowFilters(!showFilters);
  }, [showFilters]);

  const handleClearFilters = useCallback(() => {
    const selectedRowsCount = props.rowSelection?.selectedRowKeys?.length;
    if (selectedRowsCount > 0) {
      setShowWarningPopup(true);
      setPendingAction({ type: "clearFilterButtonPressed", data: {} });
    } else {
      props.clearFilters && props.clearFilters();
    }
  }, [showFilters, props.rowSelection]);

  const filterRowGutter = useMemo(() => [16], []);

  // This function is used for downloading CSV file on CSV download button click.
  // This function on the basis of type calls different saga in order to download CSV file.
  const handleDownLoadCSV = useCallback(() => {
    const _columns = columns.filter((col: any) => col.title !== "Actions" && !isValidElement(col.title));
    const partial_parsed = {
      ...partialParsed,
      [generalSearchField]: props.searchQuery
    };
    switch (downloadCSV?.type) {
      case CSVDownloadSagaType.GENERIC_CSV_DOWNLOAD:
        dispatch(
          genericTableCSVDownload(uri, method, {
            transformer: downloadCSV?.tableDataTransformer,
            filters: { ...(getFiltersState(partial_parsed) || {}), page: page - 1, page_size: pageSize },
            columns: _columns,
            derive: derive,
            shouldDerive: props.shouldDerive,
            jsxHeaders: downloadCSV?.jsxHeaders || []
          })
        );
        break;
      case CSVDownloadSagaType.TRIAGE_CSV_DOWNLOAD:
        dispatch(
          csvDownloadTriageGridView(
            "cicd_job_aggs",
            "list",
            getFiltersState(partial_parsed) || {},
            defaultTriageGridViewColumns
          )
        );
        break;
      case CSVDownloadSagaType.DRILLDOWN_CSV_DOWNLOAD:
        downloadCSV && downloadCSV?.onClickCSVDownload && downloadCSV.onClickCSVDownload();
        break;
    }
  }, [uri, method, filters, columns]);

  const _partialParsed = useMemo(
    () => ({
      [generalSearchField]: props.searchQuery
    }),
    [props.searchQuery, generalSearchField]
  );

  const isExcludedFilter = (field: string) => {
    const { filters } = props;
    if (field.includes("customfield_")) {
      return !!get(filters, ["exclude", "custom_fields", field], undefined);
    }
    return !!get(filters, ["exclude", field], undefined);
  };

  const getFilterSelectedValue = (column: any) => {
    const filters = props.filters;
    const defaultValue = column.filterType.includes("multi") ? [] : {};
    if (column.filterType === "search") {
      return partialParsed[column.filterField] || null;
    }
    if (column.prefixPath !== undefined) {
      if (isExcludedFilter(column.filterField)) {
        return get(filters, ["exclude", column.prefixPath, column.filterField], defaultValue);
      }
      return get(filters, [column.prefixPath, column.filterField], defaultValue);
    }
    if (isExcludedFilter(column.filterField)) {
      return get(filters, ["exclude", column.filterField], defaultValue);
    }
    return filters[column.filterField];
  };

  const filtersConfig = useMemo(() => {
    return columns.reduce((acc: any, column: any) => {
      if (column.filterType) {
        acc.push({
          id: column.key,
          type: column.filterType,
          field: column.filterField,
          label: column.filterLabel || column.title,
          span: column.span || null,
          uri: column.uri || null,
          options: column.options || null,
          selected: getFilterSelectedValue(column),
          apiCall: column.apiCall,
          searchField: column.searchField || "name",
          specialKey: column.specialKey || null,
          returnCall: column.returnCall,
          childMethod: column.childMethod || "values",
          fetchChildren: column.fetchChildren,
          prefixPath: column.prefixPath,
          showExcludeSwitch: column.showExcludeSwitch || false,
          excludeSwitchValue: isExcludedFilter(column.filterField)
        });
      }
      return acc;
    }, []);
  }, [props.filters, columns]);

  const renderExtras = useMemo(() => {
    if (!hasSearch && !hasFilters && !hasDelete && !extraSuffixActionButtons) {
      return null;
    }
    let hasAppliedFilters = true;
    if (!(props.filters && Object.keys(props.filters).length)) {
      const partialFiltersKeys = Object.keys(partialParsed);
      const partialFilters = partialFiltersKeys.find(key => partialParsed[key]);
      hasAppliedFilters = !!partialFilters;
    }

    return (
      <ExtraFilterContent
        hasFilters={hasFilters}
        hasDelete={hasDelete}
        hasSearch={hasSearch}
        hasAppliedFilters={hasAppliedFilters}
        generalSearchField={generalSearchField}
        onGeneralSearchHandler={handleGeneralSearchChange}
        countForAppliedFilters={getCountForAppliedFilters}
        onToggleFilters={handleFiltersClick}
        downloadCSV={downloadCSV}
        clearFilters={handleClearFilters}
        handleCSVDownload={handleDownLoadCSV}
        setShowDeletePopup={() => setShowDeletePopup(true)}
        selectedRows={props.rowSelection?.selectedRowKeys?.length || 0}
        partialParsed={_partialParsed}
        showFilters={showFilters}
        showIssueFilters={uuid.includes(`${ISSUES_UUID}@`)}
        showTriageGridFilters={uuid === TRIAGE_UUID}
        showCustomFilters={uuid === TRIAGE_UUID || uuid.includes(`${ISSUES_UUID}@`)}
        filters={filters}
        showFiltersDropDown={props.showFiltersDropDown}
        setShowFiltersDropDown={props.setShowFiltersDropDown}
        filtersConfig={filtersConfig}
        extraSuffixActionButtons={extraSuffixActionButtons}
        isDevRawStatsDrilldown={isDevRawStatsDrilldown}
      />
    );
  }, [
    hasSearch,
    hasFilters,
    filters,
    showFilters,
    columns,
    downloadCSV,
    hasDelete,
    props.rowSelection,
    props.showFiltersDropDown,
    filtersConfig
  ]);

  const genericAPIState = useParamSelector(getGenericRestAPISelector, { uri, method, uuid });

  useEffect(() => {
    dispatch(restapiClear(uri, method, uuid));
    return () => {
      dispatch(restapiClear(uri, method, uuid));
    };
  }, []);

  useEffect(() => {
    if (props.bulkDeleting && showDeletePopup) {
      setBulkDeleteLoading(true);
    }

    if (!props.bulkDeleting && showDeletePopup && bulkDeleteLoading) {
      setShowDeletePopup(false);
    }
  }, [props.bulkDeleting]);

  useEffect(() => {
    const partial_parsed = {
      ...partialParsed,
      [generalSearchField]: props.searchQuery
    };
    fetchData(partial_parsed);
  }, [filters, reload, page, props.searchQuery, props.pageSize, sortOrder]);

  useEffect(() => {
    const { loading, error } = genericAPIState;
    const selectedRowsCount = props.rowSelection?.selectedRowKeys?.length;
    if (!loading) {
      let records = get(genericAPIState, ["data", "records"], []);
      const metadata = get(genericAPIState, ["data", "_metadata"], {});
      let _cardTitle: any = (
        <div className="flex align-center">
          <div style={{ marginRight: ".3rem" }}>{props.title || "Results"} </div>
          <div style={{ marginRight: ".3rem" }}>
            <Badge
              style={{ backgroundColor: "var(--harness-blue)" }}
              count={totalRecords || 0}
              overflowCount={totalRecords || 0}
            />
          </div>
          {isDevRawStatsDrilldown ? (
            <div style={{ marginLeft: "auto", marginRight: "1rem" }}>
              <AntText className="drilldown-type">Name</AntText>
              <span className="circle-separator">{`● `}</span>
              <AntText className="drilldown-title">{fullName}</AntText>
            </div>
          ) : null}
          {selectedRowsCount !== undefined && selectedRowsCount > 0 && (
            <div className="flex align-center">
              <Tag style={{ color: "#51acff", backgroundColor: "#eaf8ff", borderColor: "#51acff", marginRight: 0 }}>
                {" "}
                {selectedRowsCount || 0} Selected
              </Tag>
              <AntButton type="link" onClick={() => props.clearSelectedIds && props.clearSelectedIds()}>
                Clear Selection
              </AntButton>
            </div>
          )}
        </div>
      );

      let extraData: any;
      if (props.componentTitle) {
        _cardTitle = props.componentTitle;
      }
      if (!showTitle) {
        _cardTitle = undefined;
      }

      if (uri === "cicd_job_aggs") {
        extraData = get(genericAPIState, ["data", "totals"], {});
      }

      if (props.transformRecords && records && records.length > 0) {
        records = props.transformRecords(records);
      }

      let _dataSource: any[];
      let _columns: any[];

      if (!dataFilter || !dataFilter.values) {
        _dataSource = records;
      } else {
        _dataSource = records.filter((row: any) => !dataFilter.values.includes(row[dataFilter.field]));
      }

      const { hasDynamicColumns } = props;
      if (!hasDynamicColumns) {
        _columns = props.columns;
      } else if (!_dataSource || _dataSource.length === 0) {
        _columns = props.columns;
      } else if (!props.renderDynamicColumns) {
        _columns = props.columns;
      } else {
        let dynamicColumns: any = [];
        _dataSource.map((record: any) => {
          const dynamicColumnKeys = Object.keys(record).filter(key => key.includes("dynamic_column_"));
          dynamicColumnKeys.map((dynamicColumn, i) => {
            const alreadyExists = dynamicColumns.find((column: any) => {
              return column.key === dynamicColumn;
            });
            if (!alreadyExists && props.renderDynamicColumns) {
              dynamicColumns.push(props.renderDynamicColumns(dynamicColumn, extraData));
            }
          });
          dynamicColumns = orderBy(dynamicColumns, ["key"], ["desc"]);
        });
        if (props.configureDynamicColumns) {
          dynamicColumns = props.configureDynamicColumns(dynamicColumns);
        }
        _columns = [...props.columns, ...dynamicColumns];
      }

      // quick fix for now
      if (uri === "quiz") {
        if (_dataSource.length && _dataSource[0].vanity_id) {
          setDataSource(_dataSource);
        } else {
          setDataSource(prev => cloneDeep(prev));
        }
      } else {
        setDataSource(_dataSource);
      }

      setTotalRecords(metadata.total_count || 0);
      setCardTitle(_cardTitle);
      setColumns(_columns);
    }
    setLoading(loading);
  }, [genericAPIState, props.columns, props.rowSelection?.selectedRowKeys, props.componentTitle]);

  useEffect(() => {
    if (
      defaultExpandIndex &&
      props.defaultExpandedRowKeys !== undefined &&
      props.defaultExpandedRowKeys !== expanded_row_index
    ) {
      setDefaultExpandIndex(false);
      setExpandedRow(props.defaultExpandedRowKeys);
    }
  }, [props.defaultExpandedRowKeys]);

  useEffect(() => {
    const hasIndex = expanded_row_index !== undefined && expanded_row_index !== null;
    if (!defaultExpandIndex && hasIndex && loading) {
      setExpandedRow(props.defaultExpandedRowKeys ? props.defaultExpandedRowKeys : undefined);
    }
  }, [loading]);

  const onExpandRowHandler = useCallback(
    (expanded: boolean, record: any) => {
      let row_key = undefined;
      if (expanded) {
        const key = rowKey;
        if (!record.hasOwnProperty(key)) {
          console.error(`Record doesn't have ${key} property.`, record);
          return;
        }
        const row = dataSource.find(item => item[key] === record[key]);
        if (!row) {
          console.error(`Row not found by ${key}.`, record);
          return;
        }
        row_key = row[key];
      }
      setExpandedRow(row_key);
    },
    [rowKey, dataSource]
  );

  const onPageSizeChangeHandler = useCallback(
    (pageSize: number) => {
      dispatch(setPage(props.pageName, { page_size: pageSize }));
      props.onPageSizeChange && props.onPageSizeChange(pageSize);
    },
    [filters]
  );

  const onSortChange = useCallback(
    (pagination: any, filters: any, sorter: any, extra: any) => {
      const sortEntry = { id: sorter.field, desc: sorter.order === "descend" };
      // * ADD this key (API_CALL_ON_TABLE_SORT_CHANGE) in column config if there is no need to call the API
      const currentColumn = (columns || []).find(column => column.key === sorter["columnKey"]);
      const fetchDataWithNewSort = get(currentColumn, [API_CALL_ON_TABLE_SORT_CHANGE], true) !== false;
      if (fetchDataWithNewSort) {
        let entries: any = {};
        entries[sorter.field] = sortEntry;
        setSortOrder(entries);
      }
    },
    [columns]
  );

  const getFiltersState = (partial_parsed: any) => {
    const { filters } = props;
    const filterKeys = Object.keys(filters);
    let finalFilters: { [key: string]: any } = {};
    if (filters && filterKeys.length) {
      finalFilters = filterKeys.reduce((acc: any, item: string) => {
        const filterConfig = columns.find((column: any) => column.filterField === item);
        if (filterConfig && filterConfig.filterType === "apiMultiSelect") {
          acc[item] = filters[item].map((filter: any) => filter.key);
          return acc;
        }
        if (filterConfig && filterConfig.filterType === "dateRange") {
          // Considering timestamp in UTC
          let $gt: number | string = filters[item]["$gt"]?.toString().includes("-")
            ? getStartOfDayFromDateString(filters[item]["$gt"])
            : filters[item]["$gt"];
          let $lt: number | string = filters[item]["$lt"]?.toString().includes("-")
            ? getEndOfDayFromDateString(filters[item]["$lt"])
            : filters[item]["$lt"];

          if (filterConfig && !filterConfig.convertToNumber) {
            $gt = $gt?.toString();
            $lt = $lt?.toString();
          }

          // updating the dateString to timestamp before the request
          acc[item] = {
            $gt,
            $lt
          };
          return acc;
        }
        if (filterConfig && filterConfig.filterType === "cascade") {
          let label: any = {};
          filters[item].forEach((l: any) => {
            let cascadeFields = l.split(" ");
            if (label[cascadeFields[0]] === undefined) {
              label[cascadeFields[0]] = [cascadeFields[1]];
            } else {
              label[cascadeFields[0]].push(cascadeFields[1]);
            }
          });
          acc[item] = label;
          return acc;
        }
        acc[item] = filters[item];
        return acc;
      }, {});
    }

    const sort =
      Object.keys(sortOrder).length > 0
        ? Object.keys(sortOrder).map((key: string) => sortOrder[key])
        : props.sort || [];

    return {
      page: page - 1 || 0, // TODO: look into
      page_size: pageSize,
      sort: sort,
      filter: {
        ...finalFilters,
        ...searchParsed,
        partial: { ...partial_parsed }
      },
      across: props.across || ""
    };
  };

  const fetchData = (partial_parsed: any) => {
    setLoading(true);
    let filters = getFiltersState(partial_parsed);
    dispatch(paginationGet(uri, method, filters, uuid, derive, shouldDerive));
  };

  const getPrefixPath = (field: string) => {
    const column = columns.find((c: any) => c.filterField === field);
    return column ? column.prefixPath : column;
  };

  const onSearchHandler = useCallback((field: string, value: any) => {
    // TODO:
    // setState(
    //   state => ({
    //     partial_parsed: {
    //       ...state.partial_parsed,
    //       [field]: value
    //     }
    //   }),
    // () => debouncedPaginationGet()
    // );
  }, []); // Pass right dependencies.

  const onOptionSelectHandlerWrapper = useCallback(
    (field: any, option: any, type: any) => {
      const selectionRowsCount = props.rowSelection?.selectedRowKeys?.length;
      if (selectionRowsCount > 0) {
        setShowWarningPopup(true);
        setPendingAction({
          type: "optionSelect",
          args: { field, option, type }
        });
      } else {
        onOptionSelectHandler(field, option, type);
      }
    },
    [props.filters, columns, props.rowSelection]
  );

  const onOptionSelectHandler = useCallback(
    (field: string, option: any, type: string = "option") => {
      const column = filtersConfig.find((c: any) => c.field === field);
      let value: any = undefined;
      if (type === "option") {
        if (option === undefined) {
          value = undefined;
        } else if (Object.keys(option).includes("value")) {
          value = option.value;
        } else {
          value = option;
        }
      } else {
        value = option;
      }

      const prefixPath = getPrefixPath(field);

      const _isDeleted = value === undefined || (Array.isArray(value) && value.length === 0);

      let _updatedFilters: any;
      const { filters } = props;

      if (_isDeleted) {
        if (prefixPath) {
          const prefixFilters = filters[prefixPath] || {};
          delete prefixFilters[field];
          _updatedFilters = {
            ...filters,
            ...prefixFilters
          };
        } else {
          const { [field]: _deletedFilter, ...restOfFilters } = filters;
          _updatedFilters = restOfFilters;
        }
      } else if (prefixPath) {
        _updatedFilters = {
          ...filters,
          [prefixPath]: {
            ...get(filters, [prefixPath], {}),
            [field]: value
          }
        };
      } else {
        _updatedFilters = {
          ...filters,
          [field]: value
        };
      }

      if (get(column, ["excludeSwitchValue"], false)) {
        _updatedFilters = buildServerPaginatedQuery(
          _updatedFilters,
          field,
          column.filterType,
          column.excludeSwitchValue
        );
      }
      handleFilterChange(_updatedFilters);
    },
    [props.filters, columns]
  );

  const onExcludeSwitchChangeWrapper = useCallback(
    (field: string, value: any) => {
      const selectionRowsCount = props.rowSelection?.selectedRowsKeys?.length;
      if (selectionRowsCount > 0) {
        setShowWarningPopup(true);
        setPendingAction({
          type: "excludeSwitch",
          args: { field, value }
        });
      } else {
        onExcludeSwitchChange(field, value);
      }
    },
    [props.filters, columns, props.rowSelection]
  );

  const onExcludeSwitchChange = useCallback(
    (field: string, value: any) => {
      const column = columns.find((c: any) => c.filterField === field);
      const newFilters = buildServerPaginatedQuery(props.filters || {}, field, column.filterType, value);
      handleFilterChange(newFilters);
    },
    [props.filters, columns, props.rowSelection]
  );

  const onHandleFieldFilterChange = useCallback(
    (field: string, value: any) => {
      const selectionRowsCount = props.rowSelection?.selectedRowKeys?.length;
      if (selectionRowsCount > 0) {
        setShowWarningPopup(true);
        setPendingAction({
          type: "inputChange",
          args: { field, value }
        });
      } else {
        handleFieldFilterChange(field, value);
      }
    },
    [props.filters, props.rowSelection, columns]
  );

  const handleFieldFilterChange = useCallback(
    (field: string, value: any) => {
      handleFilterChange({
        ...props.filters,
        [field]: value
      });
    },
    [props.filters]
  );

  const handleFilterChange = (filters: any) => {
    props.onFiltersChange && props.onFiltersChange(filters);
  };

  const totalPages = props.pagination_total ? Math.ceil(props.pagination_total / pageSize) * 2 : 1;

  const renderFilters = useMemo(() => {
    if (!showFilters) {
      return null;
    }
    return (
      <TableFiltersContainer
        key="filters"
        filtersConfig={filtersConfig}
        onSearchEvent={onSearchHandler}
        onOptionSelectEvent={onOptionSelectHandlerWrapper}
        onInputChange={onHandleFieldFilterChange}
        onTagsChange={onHandleFieldFilterChange}
        onExcludeSwitchChange={onExcludeSwitchChangeWrapper}
        onCloseFilters={handleFiltersClick}
      />
    );
  }, [showFilters, filtersConfig, filters, props.rowSelection]);

  const memoizedExpandedRow = useMemo(() => [expanded_row_index], [expanded_row_index]);

  const popupOnCancel = () => {
    if (showDeletePopup) {
      setShowDeletePopup(false);
    } else {
      setShowWarningPopup(false);
      setPendingAction({});
    }
  };

  const popupOnOk = () => {
    if (showDeletePopup) {
      props.onBulkDelete && props.onBulkDelete();
    } else {
      setShowWarningPopup(false);
      props.clearSelectedIds && props.clearSelectedIds();
      doPendingAction();
    }
  };

  const doPendingAction = () => {
    if (Object.keys(pendingAction).length > 0 && pendingAction.hasOwnProperty("type")) {
      switch (pendingAction.type) {
        case "generalSearch":
          {
            const { value } = pendingAction.args;
            generalSearchField && props.onSearchChange && props.onSearchChange(value);
            setPendingAction({});
          }
          break;
        case "clearFilterButtonPressed":
          props.clearFilters && props.clearFilters();
          setPendingAction({});
          break;
        case "optionSelect":
          {
            const { field, option, type } = pendingAction.args;
            onOptionSelectHandler(field, option, type);
          }
          break;
        case "inputChange":
          {
            const { field, value } = pendingAction.args;
            handleFieldFilterChange(field, value);
          }
          break;
        case "excludeSwitch": {
          const { field, value } = pendingAction.args;
          onExcludeSwitchChange(field, value);
        }
        default:
      }
      setPendingAction({});
    }
  };

  const className = useMemo(
    () => (uuid === TRIAGE_UUID || uuid.includes(`${ISSUES_UUID}@`) ? "triage-grid-view-list" : ""),
    [uuid]
  );

  const showPagination = useMemo(() => {
    return !loading;
  }, [loading]);

  const showCustomChanger = useMemo(() => {
    return !loading && totalRecords < pageSize;
  }, [loading, totalRecords, pageSize]);

  const GraphComponent = props.extraPropsForGraph;
  return (
    <>
      {useRestTableAddFilters && showFilters && (
        <RestTableAddFilterContainer
          key="rest-table-filters"
          filtersConfig={restTableFilterConfigs ?? []}
          onOptionSelectEvent={onOptionSelectHandlerWrapper}
          onCloseFilters={handleFiltersClick}
          filterSaveButtonEnabled={props.filterSaveButtonEnabled}
          handleFilterSave={props.handleFilterSave}
          allFilters={props.filters ?? {}}
          savingFilters={props.savingFilters}
        />
      )}
      {uuid.includes(`${ISSUES_UUID}@`) && showFilters && (
        <NewTableFiltersContainer
          key="filters"
          filtersConfig={filtersConfig}
          onSearchEvent={onSearchHandler}
          onOptionSelectEvent={onOptionSelectHandlerWrapper}
          onInputChange={onHandleFieldFilterChange}
          onTagsChange={onHandleFieldFilterChange}
          onExcludeSwitchChange={onExcludeSwitchChangeWrapper}
          onCloseFilters={handleFiltersClick}
          filterSaveButtonEnabled={props.filterSaveButtonEnabled}
          handleFilterSave={props.handleFilterSave}
          savingFilters={props.savingFilters}
        />
      )}
      <Card className={className} title={cardTitle} bordered={false} extra={renderExtras} bodyStyle={cardBodyStyle}>
        <div>
          {uuid !== TRIAGE_UUID && !uuid.includes(`${ISSUES_UUID}@`) && renderFilters}
          {extraPropsForGraph && GraphComponent}
          <PaginatedTable
            hasCustomPagination={showPagination}
            className={cx({ "server-paginated-table-bordered": props.bordered })}
            dataSource={dataSource}
            loading={loading}
            expandedRowRender={props.expandedRowRender}
            expandedRowKeys={props.expandedRowKeys || memoizedExpandedRow}
            pageSize={pageSize}
            page={page}
            columns={visibleColumns}
            rowSelection={props.rowSelection}
            totalPages={totalPages}
            totalRecords={totalRecords}
            bordered={props.bordered}
            scroll={props.scroll}
            size="middle"
            rowKey={rowKey}
            onExpand={onExpandRowHandler}
            onPageChange={props.onPageChange}
            onPageSizeChange={onPageSizeChangeHandler}
            onRow={props.onRow}
            onChange={onSortChange}
            rowClassName={props.rowClassName}
            showPageSizeOptions={props.showPageSizeOptions === undefined ? true : props.showPageSizeOptions}
            expandIconColumnIndex={props.expandIconColumnIndex}
            locale={{
              emptyText: loading ? (
                <div className="rest-table-spinner">
                  <Spin />
                </div>
              ) : (
                ""
              )
            }}
            showCustomChanger={showCustomChanger}
          />
          <PaginatedSelectPopup
            onCancel={popupOnCancel}
            onOk={popupOnOk}
            warningVisible={showWarningPopup}
            deleteVisible={showDeletePopup}
            selectedItemCount={props.rowSelection?.selectedRowKeys?.length || 0}
            bulkDeleting={props.bulkDeleting}
          />
        </div>
      </Card>
    </>
  );
};

export default ErrorWrapper(RestApiPaginatedTable);
