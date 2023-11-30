import { Badge, Card, Icon, Spin, Tag } from "antd";
import { DEFAULT_PAGE_SIZE } from "constants/pageSettings";
import { getKeyForFilter } from "dashboard/constants/helper";
import ErrorWrapper from "hoc/errorWrapper";
import {
  capitalize,
  clone,
  cloneDeep,
  debounce,
  filter,
  find,
  findIndex,
  get,
  isEqual,
  orderBy,
  set,
  uniq,
  unset
} from "lodash";
import * as PropTypes from "prop-types";
import React, { Component, isValidElement } from "react";
import { connect } from "react-redux";
import { genericTableCSVDownload, widgetDrilldownColumnsUpdate } from "reduxConfigs/actions/restapi";
import { mapPaginationDispatchtoProps, mapPaginationStatetoProps } from "reduxConfigs/maps/paginationMap";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { AntButton, AntTable, AntInput } from "shared-resources/components";
import { removeEmptyKeys, sanitizeObject } from "utils/commonUtils";
import { removeFilterKey } from "../../../dashboard/components/dashboard-application-filters/AddFiltersComponent/helpers";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { genericPaginationData, genericPaginationSeachData } from "reduxConfigs/selectors/restapiSelector";
import { Filters } from "./containers";
import CustomFiltersContainer from "./containers/filters/custom-filters-container";
import { ExtraFilterContent } from "./extra-filter-content";
import { buildMissingFieldsQuery, buildServerPaginatedQuery, hasFilterValue, sortingColumnForFixedLeft } from "./helper";
import { PaginatedSelectPopup } from "./paginated-table-select-popup.component";
import { API_CALL_ON_TABLE_SORT_CHANGE } from "constants/pageSettings";
import { DRILLDOWN_UUID } from "../../../dashboard/pages/dashboard-drill-down-preview/helper";
import DrillDownFilterContent from "./components/drilldown-filter-content/drilldown-filter-content";
import { dynamicColumnPrefix } from "../../../custom-hooks/helpers/leadTime.helper";
import NewTableFiltersContainer from "./containers/filters/new-table-filters.container";
import "./rest-api-paginated-table.style.scss";
import cx from "classnames";
import widgetConstants from "dashboard/constants/widgetConstants";
import { EmptyApiErrorWidget } from "../../components";
import memoizeOne from "memoize-one";
import { getServerErrorDesc } from "./../../helpers/server-error/server-error-helper";
import { ServerErrorSource, ERROR_CODE_RANGE_START } from "./../../helpers/server-error/constant";
import { toTitleCase } from "utils/stringUtils";
import {
  DORA_REPORTS,
  LEAD_TIME_REPORTS,
  LEAD_MTTR_DORA_REPORTS,
  TESTRAILS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";

class ServerPaginatedTable extends Component {
  waitFunc = debounce(() => {}, 100);

  constructor(props) {
    super(props);
    const pageName = props.pageName;
    const pageSettings = props.page_settings ? props.page_settings[pageName] : undefined;
    this.state = {
      page_data: [],
      page: 1, // Page of the AntTable, not paginated request
      pageSize: pageSettings ? pageSettings.page_size || props.pageSize : props.pageSize,
      filters: {
        page: 0,
        page_size: 100,
        sort: {}
      },
      search: "",
      search_parsed: {},
      partial_parsed: {},
      loading: true,
      num_pages: 1,
      search_valid: true,
      search_error: "",
      more_filters: this.props.moreFilters,
      delay: this.props.delay === undefined ? 0 : this.props.delay,
      expanded_row_keys: undefined,
      filtersDisplayed: false,
      reload: props.reload,
      sort_order: {},
      setDefaultExpandIndex: true,
      showWarningPopup: false,
      showDeletePopup: false,
      bulkDeleteLoading: false,
      showFiltersDropDown: false,
      activeColumn: this.props.activeColumn,
      clearFiltersCount: this.props.clearFiltersCount
    };
    this.handleSearch = this.handleSearch.bind(this);
    this.updateQueryValid = this.updateQueryValid.bind(this);
    this.fetchData = this.fetchData.bind(this);
    this.compareFilters = this.compareFilters.bind(this);
    this.onPageSizeChangeHandler = this.onPageSizeChangeHandler.bind(this);
    this.onPageChangeHandler = this.onPageChangeHandler.bind(this);
    this.onExpandRowHandler = this.onExpandRowHandler.bind(this);

    this.onToggleFilters = this.onToggleFilters.bind(this);
    this.getFiltersState = this.getFiltersState.bind(this);
    this.checkForAppliedFilters = this.checkForAppliedFilters.bind(this);
    this.getCountForAppliedFilters = this.getCountForAppliedFilters.bind(this);

    this.getFiltersConfig = this.getFiltersConfig.bind(this);
    this.onSearchHandler = this.onSearchHandler.bind(this);
    this.onOptionSelectHandler = this.onOptionSelectHandler.bind(this);
    this.debouncedPaginationGet = debounce(this.getPaginationData, 500);
    this.getFilterSelectedValue = this.getFilterSelectedValue.bind(this);
    this.isClearFilters = this.isClearFilters.bind(this);
    this.onGeneralSearchHandler = this.onGeneralSearchHandler.bind(this);
    this.getPrefixPath = this.getPrefixPath.bind(this);
    this.onSortChange = this.onSortChange.bind(this);
    this.onInputChange = this.onInputChange.bind(this);
    this.onTagsChange = this.onTagsChange.bind(this);
    this.onExcludeSwitchChange = this.onExcludeSwitchChange.bind(this);
    this.isExcludedFilter = this.isExcludedFilter.bind(this);
    this.onCheckBoxValueChange = this.onCheckBoxValueChange.bind(this);
    this.updateFiltersAndReload = this.updateFiltersAndReload.bind(this);
    this.onBinaryChange = this.onBinaryChange.bind(this);
    this.handleCSVDownload = this.handleCSVDownload.bind(this);
    this.clearFilters = this.clearFilters.bind(this);
    this.popupOnCancel = this.popupOnCancel.bind(this);
    this.popupOnOk = this.popupOnOk.bind(this);
    this.doPendingAction = this.doPendingAction.bind(this);
    // this.selectionWarningWrapper = this.selectionWarningWrapper.bind(this);
    this.onOptionSelectHandlerWrapper = this.onOptionSelectHandlerWrapper.bind(this);
    this.onInputChangeWrapper = this.onInputChangeWrapper.bind(this);
    this.onExcludeSwitchChangeWrapper = this.onExcludeSwitchChangeWrapper.bind(this);
    this.onCheckBoxValueChangeWrapper = this.onCheckBoxValueChangeWrapper.bind(this);
    this.onBinaryChangeWrapper = this.onBinaryChangeWrapper.bind(this);
    this.onRemoveFilter = this.onRemoveFilter.bind(this);
    this.setShowFiltersDropDown = this.setShowFiltersDropDown.bind(this);
    this.onInputFieldChangeHandlerWrapper = this.onInputFieldChangeHandlerWrapper.bind(this);
    this.deleteButtonhandler = this.deleteButtonhandler.bind(this);
    // just move this to use the restapi action that is passed. for data,
    // lets use the method and uri to get the results

    //this.props.paginationSet(this.props.uri, this.props.method, this.props.cancelToken);

    // here instead of paginationSet of backend call, set it as an action and do it that way
    //this.props.paginationSet(this.props.action);
    // use props for method=list or search, uri=something, method=method
  }

  compareFilters(filter1, filter2) {
    if (filter1 === undefined || filter1 === null || filter2 === undefined || filter2 === null) {
      return true;
    }

    let newFilter1 = JSON.parse(JSON.stringify(filter1));
    let newFilter2 = JSON.parse(JSON.stringify(filter2));

    Object.keys(newFilter1).forEach(key => {
      if (!newFilter2.hasOwnProperty(key)) {
        return false;
      }
      if (Array.isArray(newFilter1[key])) {
        let array1 = newFilter1[key];
        let array2 = newFilter2[key];
        if (array1.length !== array2.length) {
          return false;
        }
        array1.forEach(item => {
          if (array2.filter(i => i === item).length === 0) {
            return false;
          }
        });
      } else {
        if (newFilter1[key] !== newFilter2[key]) {
          return false;
        }
      }

      delete newFilter1[key];
      delete newFilter2[key];
    });

    if (Object.keys(newFilter1).length > 0 || Object.keys(newFilter2).length > 0) {
      return false;
    }

    return !(Object.keys(newFilter1).length > 0 || Object.keys(newFilter2).length > 0);
  }

  componentWillMount() {
    this.props.restapiClear(this.props.uri, this.props.method, this.props.uuid);
    // debounce the mount by 2 milliseconds to avoid timing issues
    this.waitFunc();
  }

  componentDidMount() {
    this.fetchData(this.state);
  }

  componentWillReceiveProps(nextProps) {
    if (!isEqual(nextProps.moreFilters, this.props.moreFilters)) {
      this.setState(
        {
          more_filters: nextProps.moreFilters
        },
        () => {
          const filters = this.getFiltersState(this.state);
          const { uri, method, uuid, derive, shouldDerive, report, queryParam } = nextProps;
          filters.page = 0;
          this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
          this.setState({ page: 1 });
          //this.props.genericList(uri, method, filters);
          this.waitFunc();
        }
      );
    }

    if (!isEqual(nextProps.partialFilters, this.props.partialFilters)) {
      this.setState({
        more_filters: nextProps.moreFilters
      });
    }

    const pageName = nextProps.pageName;
    const pageSettings = nextProps.page_settings ? nextProps.page_settings[pageName] : undefined;
    if (pageSettings && pageSettings.page_size !== this.state.pageSize) {
      this.setState({
        pageSize: pageSettings.page_size
      });
    }

    if (nextProps.reload !== this.state.reload) {
      this.setState({ reload: nextProps.reload }, () => {
        const filters = this.getFiltersState(this.state);
        const { uri, method, uuid, derive, shouldDerive, report, queryParam } = nextProps;
        filters.page = 0;
        this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
        this.setState({ page: 1 });
        //this.props.genericList(uri, method, filters);
        this.waitFunc();
      });
    }
    if (nextProps.clearFiltersCount !== this.state.clearFiltersCount) {
      this.setState(
        {
          clearFiltersCount: nextProps.clearFiltersCount
        },
        () => {
          this.clearFilters();
        }
      );
    }
    if (
      this.state.setDefaultExpandIndex &&
      nextProps.defaultExpandedRowKeys !== undefined &&
      nextProps.defaultExpandedRowKeys !== this.state.expanded_row_keys
    ) {
      this.setState({ expanded_row_keys: nextProps.defaultExpandedRowKeys, setDefaultExpandIndex: false });
    }

    const rowIndex = this.state.expanded_row_keys;
    const hasIndex = rowIndex !== undefined && rowIndex !== null;
    if (!this.state.setDefaultExpandIndex && hasIndex && this.props.paginationState.loading) {
      this.setState({
        expanded_row_keys: nextProps.defaultExpandedRowKeys !== "" ? nextProps.defaultExpandedRowKeys : ""
      });
    }

    if (this.state.showDeletePopup && this.props.bulkDeleting) {
      this.setState({ bulkDeleteLoading: true });
    }

    if (this.state.showDeletePopup && !this.state.bulkDeleting && this.state.bulkDeleteLoading) {
      this.setState({ showDeletePopup: false, bulkDeleteLoading: false });
    }

    if (!isEqual(this.props.activeColumn, nextProps.activeColumn)) {
      this.setState({ activeColumn: nextProps.activeColumn });
    }
  }

  componentDidUpdate(prevProps, prevState) {
    // if (!this.compareFilters(prevState.more_filters, this.state.more_filters)) {
    //     const filters = this.getFiltersState(this.state);
    //     filters.page = 0;
    //     this.props.paginationGet(filters);
    //     this.waitFunc();
    // }
    // if (!this.compareFilters(prevState.partial_parsed, this.state.partial_parsed)) {
    //     this.debouncedPaginationGet();
    // }
    if (!this.compareFilters(prevState.more_filters, this.state.more_filters)) {
      const filters = this.getFiltersState(this.state);
      if (this.props.onFiltersChanged) {
        this.props.onFiltersChanged(filters);
      }
    }
  }

  componentWillUnmount() {
    //this.props.paginationClear();
    this.props.restapiClear(this.props.uri, this.props.method, "0");
    this.setState({
      partial_parsed: {},
      more_filters: {}
    });
  }

  getPaginationData() {
    let filters = this.getFiltersState(this.state);
    filters.page = 0;
    let { uri, method, uuid, derive, shouldDerive, report, queryParam, searchURI } = this.props;
    if (this.state.partial_parsed && this.state.partial_parsed[this.props.generalSearchField] && searchURI) {
      uri = searchURI;
    }
    if (this.props?.generalSearchPartialKey) {
      const partialMatchFilter = get(filters, ["filter", "partial"], {});
      if (Object.keys(partialMatchFilter).length) {
        const key = Object.keys(partialMatchFilter)[0];
        const value = partialMatchFilter[key];
        set(filters, ["filter", "partial", key], { [this.props?.generalSearchPartialKey]: value });
      }
    }
    this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
    this.setState({ page: 1 });
    //this.props.genericList(uri, method, filters);
  }

  onExpandRowHandler(expanded, record) {
    let rowKey = undefined;
    if (expanded) {
      const key = this.props.rowKey;
      if (!record.hasOwnProperty(key)) {
        console.error(`Record doesn't have ${key} property.`, record);
        return;
      }
      const data = this.filteredPaginationData;
      const row = data.find(item => item[key] === record[key]);
      if (!row) {
        console.error(`Row not found by ${key}.`, record);
        return;
      }
      rowKey = row[key];
    }
    this.setState({ expanded_row_keys: rowKey });
  }

  onPageSizeChangeHandler(pageSize) {
    this.setState(
      {
        pageSize
      },
      () => {
        this.props.setPage(this.props.pageName, { page_size: pageSize });
        const filters = this.getFiltersState(this.state);
        filters.page = 0;
        const { uri, method, uuid, derive, shouldDerive, report, queryParam } = this.props;
        this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
        this.setState({ page: 1 });
        //this.props.genericList(uri, method, filters);
      }
    );
  }

  onPageChangeHandler(page) {
    this.setState(
      {
        page
      },
      () => this.fetchData(this.state)
    );
  }

  mapFiltersBeforeCall = filters => {
    const _mapFilters = get(this.props, ["mapFiltersBeforeCall"], undefined);
    const shouldMapFilters = !!_mapFilters;
    if (shouldMapFilters) {
      if ([LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT, JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT].includes(this.props.report)) {
        const excludeStageValues = get(this.props.widgetMetaData, ["hide_stages"], undefined);
        return sanitizeObject(_mapFilters(filters, excludeStageValues));
      }
      return sanitizeObject(_mapFilters(filters));
    }
    return sanitizeObject(filters);
  };

  fetchData(state, instance) {
    let filters = this.getFiltersState(state);
    let { uri, method, uuid, derive, shouldDerive, report, queryParam, searchURI } = this.props;
    if (this.state.partial_parsed && this.state.partial_parsed[this.props.generalSearchField] && searchURI) {
      uri = searchURI;
    }
    this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
    //this.props.genericList(uri, method, filters);
  }

  handleSearch(e) {
    const filters = this.getFiltersState(this.state);
    const { uri, method, uuid, derive, shouldDerive, report, queryParam } = this.props;
    this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive, null, { report, queryParam });
    //this.props.genericList(uri, method, filters);
  }

  updateQueryValid(query, result, id = null) {
    if (result.isError) {
      this.setState({
        search: query,
        search_valid: false,
        search_error: result.name
      });
    } else {
      // now parse the query and add it to the filters
      //let search_parsed = this.state.search_parsed;
      //let partial_parsed = this.state.partial_parsed;
      let search_parsed = {};
      let partial_parsed = {};
      result.forEach(record => {
        if (record.operator === "=") {
          search_parsed[record.category] = record.value;
        } else {
          partial_parsed[record.category] = record.value;
        }
      });
      this.setState({
        search_parsed: search_parsed,
        partial_parsed: partial_parsed,
        search: query,
        search_valid: true,
        search_error: ""
      });
    }
  }

  get isLoading() {
    return !!this.props.paginationState.loading;
  }

  get filteredPaginationData() {
    const { paginationState, transformRecordsData, paginatedSearchState, searchURI } = this.props;
    let loading = true,
      data = {};

    if (this.state.partial_parsed && this.state.partial_parsed[this.props.generalSearchField] && searchURI) {
      loading = get(paginatedSearchState, ["loading"], true);
      data = get(paginatedSearchState, ["data"], {});
    } else {
      loading = paginationState.loading !== undefined ? paginationState.loading : true;
      data = paginationState.data || {};
    }

    const records = transformRecordsData ? transformRecordsData(data) : data.records || [];

    if (loading) {
      return [];
    }
    // TODO: Fix it later.
    if (this.props.uri === "cicd_job_aggs") {
      records.forEach(record => {
        if (record && record.aggs && record.aggs.length) {
          record.aggs.forEach(({ key, totals }) => {
            record[`dynamic_column_aggs_${key}`] = totals;
          });
        }
      });
    } else if (this.props.uri === "lead_time_values") {
      records.forEach(record => {
        if (record && record.data && record.data.length) {
          record.data.forEach(({ key, mean }) => {
            record[`dynamic_column_aggs_${key}`] = mean;
          });
        }
      });
    } else if (this.props.uri === "org_users") {
      records.forEach(record => {
        const additionalData = get(record, "additional_fields", {});
        if (Object.keys(additionalData).length) {
          Object.keys(additionalData).forEach(key => {
            record[`${dynamicColumnPrefix}${key}`] = get(additionalData, key, undefined);
          });
        }
      });
    }
    const dataFilter = this.props.dataFilter;
    const dataResult = records.filter(row => !dataFilter.values.includes(row[dataFilter.field]));

    return dataResult;
  }

  getDrillDownColumnsList() {
    let drilldownColumns = get(widgetConstants, [this.props.report, "drilldown", "columns"], []);

    if ([...Object.values(DORA_REPORTS), ...Object.values(LEAD_MTTR_DORA_REPORTS), TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT, TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT].includes(this.props.report)) {
      const drilldownColumnTransformFunction = get(
        widgetConstants,
        [this.props.report, "drilldown", "drilldownColumnTransformer"],
        []
      );

      if (drilldownColumnTransformFunction) {
        let categoryColorMapping;
        drilldownColumns = drilldownColumnTransformFunction({
          columns: drilldownColumns,
          categoryColorMapping,
          filters: {
            integrationType: this.props.doraProfileIntegrationType || "",
            integrationApplication: this.props.doraProfileIntegrationApplication || "",
            query: this.props.moreFilters,
          },
          doraProfileDeploymentRoute: this.props.doraProfileDeploymentRoute,
          doraProfileEvent: this.props.doraProfileEvent
        });
      }
    }

    let getCustomColumn = get(widgetConstants, [this.props.report, "drilldown", "getCustomColumn"], []);
    if ([TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT].includes(this.props.report) && getCustomColumn) {
      let customFiled = getCustomColumn(this.props.testrailsCustomField);
      drilldownColumns = [...drilldownColumns, ...customFiled];
    }

    return [...drilldownColumns, ...this.dynamicColumns]
      .filter(column => !column.hidden)
      .map(column => {
        let title = typeof column.title === "string" ? column.title : column.titleForCSV;
        if (!title) {
          title = toTitleCase(column.dataIndex);
        }
        return {
          title,
          dataIndex: column.dataIndex
        };
      });
  }

  getFilterSelectedValue(column) {
    const defaultValue = column.filterType.includes("multi") ? [] : {};
    if (column.filterType === "search") {
      return this.state.partial_parsed[column.filterField] || null;
    }
    if (
      column.filterType === "apiMultiSelect" &&
      ["workitem_sprint_full_names", "teams", "code_area"].includes(column.filterField)
    ) {
      if (column.filterField === "workitem_sprint_full_names") {
        return get(this.state.more_filters, [column.filterField], []).map(item => {
          if (typeof item === "string") {
            return { label: item, key: item };
          }
          return item;
        });
      }
      return get(this.state.more_filters, ["workitem_attributes", column.filterField], []).map(item => {
        if (typeof item === "string") {
          return { label: item, key: item };
        }
        return item;
      });
    }
    if (column.prefixPath !== undefined) {
      if (this.isExcludedFilter(column.filterField)) {
        return get(this.state.more_filters, ["exclude", column.prefixPath, column.filterField], defaultValue);
      }
      return get(this.state.more_filters, [column.prefixPath, column.filterField], defaultValue);
    }
    if (this.isExcludedFilter(column.filterField)) {
      return get(this.state.more_filters, ["exclude", column.filterField], defaultValue);
    }
    if (column.filterType === "partial_match") {
      return get(this.state.more_filters, ["partial_match", column.filterField]);
    }
    return this.state.more_filters[column.filterField];
  }

  isExcludedFilter(field) {
    const { more_filters } = this.state;
    if (field?.includes("customfield_")) {
      return !!get(more_filters, ["exclude", "custom_fields", field], undefined);
    }
    return !!get(more_filters, ["exclude", field], undefined);
  }

  getFiltersConfig() {
    const currentColumns = this.props.allColumns || this.columns;
    return currentColumns.reduce((acc, column) => {
      if (column.filterType) {
        acc.push({
          ...column,
          id: column.key,
          type: column.filterType,
          field: column.filterField,
          label: column.filterLabel || column.title,
          span: column.span || null,
          uri: column.uri || null,
          options: column.options || null,
          selected: this.getFilterSelectedValue(column),
          apiCall: column.apiCall,
          searchField: column.searchField || "name",
          specialKey: column.specialKey || null,
          returnCall: column.returnCall,
          childMethod: column.childMethod || "values",
          fetchChildren: column.fetchChildren,
          prefixPath: column.prefixPath,
          showExcludeSwitch: column.showExcludeSwitch || false,
          excludeSwitchValue: this.isExcludedFilter(column.filterField || ""),
          showCheckboxes: column.showCheckboxes || false,
          rangeDataType: column.rangeDataType || "number",
          unlimitedLength: column.unlimitedLength || false,
          morePayload: column.morePayload || {},
          moreFilters: column.moreFilters || {},
          transformOptions: column.transformOptions,
          transformPayload: column.transformPayload,
          hasNewRecordsFormat: column.hasNewRecordsFormat || false
        });
      }
      return acc;
    }, []);
  }

  onToggleFilters() {
    this.setState(state => ({
      filtersDisplayed: !state.filtersDisplayed
    }));
  }
  setShowFiltersDropDown(val) {
    this.setState(state => ({
      showFiltersDropDown: val
    }));
  }
  onBinaryChangeWrapper(field, value) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState({
        showWarningPopup: true,
        pendingAction: { type: "binary", args: { field, value } }
      });
    } else {
      this.onBinaryChange(field, value);
    }
  }

  onBinaryChange(field, value) {
    this.setState(
      state => {
        if (value === "all") {
          delete state.more_filters[field];
          return {
            ...state
          };
        } else {
          return {
            ...state,
            more_filters: {
              ...state.more_filters,
              [field]: value
            }
          };
        }
      },
      () => this.debouncedPaginationGet()
    );
  }

  onSearchHandler(field, value) {
    const selectedRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectedRowsCount > 0) {
      this.setState(state => ({
        showWarningPopup: true,
        pendingAction: {
          type: "generalSearch",
          data: {
            partial_parsed: {
              ...state.partial_parsed,
              [field]: value
            }
          }
        }
      }));
    } else {
      if (value?.length) {
        this.setState(
          state => ({
            partial_parsed: {
              ...state.partial_parsed,
              [field]: value
            }
          }),
          () => this.debouncedPaginationGet()
        );
      } else {
        const partial_parsed = { ...this.state.partial_parsed };
        delete partial_parsed?.[field];
        this.setState(
          state => ({
            partial_parsed: partial_parsed
          }),
          () => this.debouncedPaginationGet()
        );
      }
    }
  }

  onInputChangeWrapper(field, value) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState({
        showWarningPopup: true,
        pendingAction: { type: "inputChange", args: { field, value } }
      });
    } else {
      this.onInputChange(field, value);
    }
  }

  onInputChange(field, value) {
    this.setState(
      state => ({
        more_filters: {
          ...state.more_filters,
          [field]: value
        }
      }),
      () => {
        if (this.props.onFiltersChange) {
          this.props.onFiltersChange(this.state.more_filters);
        }
        this.debouncedPaginationGet();
      }
    );
  }

  onTagsChange(field, value) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState(state => {
        return {
          showWarningPopup: true,
          pendingAction: {
            type: "generalSearch",
            data: {
              more_filters: {
                ...state.more_filters,
                [field]: value
              }
            }
          }
        };
      });
    } else {
      this.setState(
        state => ({
          more_filters: {
            ...state.more_filters,
            [field]: value
          }
        }),
        () => this.debouncedPaginationGet()
      );
    }
  }

  onGeneralSearchHandler(e) {
    const { generalSearchField } = this.props;
    if (this.props?.setSearchValue) {
      this.props?.setSearchValue(e?.target?.value);
    }
    if (generalSearchField && e.target) {
      const { value } = e.target;
      const selectedRowsCount = this.props.rowSelection?.selectedRowKeys?.length || 0;
      if (selectedRowsCount > 0 && !this.props.skipConfirmationDialog) {
        this.setState(state => {
          return {
            showWarningPopup: true,
            pendingAction: {
              type: "generalSearch",
              data: {
                partial_parsed: {
                  ...state.partial_parsed,
                  [generalSearchField]: value
                }
              }
            }
          };
        });
      } else {
        if (value?.length) {
          this.setState(
            state => {
              return {
                partial_parsed: {
                  ...state.partial_parsed,
                  [generalSearchField]: value
                }
              };
            },
            () => this.debouncedPaginationGet()
          );
        } else {
          const partial_parsed = { ...this.state.partial_parsed };
          delete partial_parsed?.[generalSearchField];
          this.setState(
            state => {
              return {
                partial_parsed: partial_parsed
              };
            },
            () => this.debouncedPaginationGet()
          );
        }
      }
    }
  }

  getPrefixPath(field) {
    const column = this.getFiltersConfig().find(c => c.field === field);
    if (column) {
      return column.prefixPath;
    } else {
      return column;
    }
  }

  onInputFieldChangeHandlerWrapper(field, value, type) {
    const selectedRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectedRowsCount > 0) {
      this.setState(state => ({
        showWarningPopup: true,
        pendingAction: {
          type: "partial_match",
          data: {
            more_filters: {
              ...state.more_filters,
              partial_match: {
                ...state.partial_match,
                [field]: { [type]: value }
              }
            }
          }
        }
      }));
    } else {
      this.setState(
        state => {
          return {
            more_filters: {
              ...state.more_filters,
              partial_match: {
                ...state.partial_match,
                [field]: { [type]: value }
              }
            }
          };
        },
        () => this.debouncedPaginationGet()
      );
    }
  }

  deleteButtonhandler() {
    if (this.props.bulkDeleteRestriction) {
      this.props.onBulkDelete();
    } else {
      this.setState({ showDeletePopup: true });
    }
  }

  onOptionSelectHandlerWrapper(field, option, type) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState({
        showWarningPopup: true,
        pendingAction: { type: "optionSelect", args: { field, option, type } }
      });
    } else {
      this.onOptionSelectHandler(field, option, type);
    }
  }

  onOptionSelectHandler(field, option, type = "option") {
    const column = this.getFiltersConfig().find(c => c.field === field);
    let value = undefined;
    if (type === "option") {
      if (option === undefined) {
        value = undefined;
      } else if (Object.keys(option).includes("value")) {
        value = option.value;
      } else if (["teams", "code_area"].includes(column.field)) {
        value = option.map(item => item.key);
      } else {
        value = option;
      }
    } else {
      value = option;
    }

    const prefixPath = this.getPrefixPath(field);

    const _isDeleted = value === undefined || (Array.isArray(value) && value.length === 0);

    this.setState(
      state => {
        let _updatedFilters;

        if (_isDeleted) {
          if (prefixPath) {
            const prefixFilters = state.more_filters[prefixPath] || {};
            delete prefixFilters[field];
            _updatedFilters = {
              ...state.more_filters,
              ...prefixFilters
            };
          } else {
            const { [field]: _deletedFilter, ...restOfFilters } = state.more_filters;
            _updatedFilters = restOfFilters;
          }
        } else if (prefixPath) {
          _updatedFilters = {
            ...state.more_filters,
            [prefixPath]: {
              ...get(state.more_filters, [prefixPath], {}),
              [field]: value
            }
          };
        } else {
          _updatedFilters = {
            ...state.more_filters,
            [field]: value
          };
        }
        if (get(column, ["excludeSwitchValue"], false)) {
          _updatedFilters = buildServerPaginatedQuery(_updatedFilters, field, column.type, column.excludeSwitchValue);
        }

        return {
          page: 1,
          more_filters: _updatedFilters
        };
      },
      () => {
        if (this.props.onFiltersChange) {
          this.props.onFiltersChange(this.state.more_filters);
        }

        this.fetchData(this.state);
      }
    );
  }

  updateFiltersAndReload = newFilters => {
    this.setState({ more_filters: newFilters, page: 1 }, () => {
      if (this.props.onFiltersChange) {
        this.props.onFiltersChange(this.state.more_filters);
      }
      this.fetchData(this.state);
    });
  };

  onExcludeSwitchChangeWrapper(field, value) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState({
        showWarningPopup: true,
        pendingAction: { type: "excludeSwitch", args: { field, value } }
      });
    } else {
      this.onExcludeSwitchChange(field, value);
    }
  }

  onExcludeSwitchChange(field, value) {
    const column = this.columns.find(c => c.filterField === field);
    const newFilters = buildServerPaginatedQuery(this.state.more_filters || {}, field, column.filterType, value);
    this.updateFiltersAndReload(newFilters);
  }

  onCheckBoxValueChangeWrapper(field, key, value) {
    const selectionRowsCount = this.props.rowSelection?.selectedRowKeys?.length;
    if (selectionRowsCount > 0) {
      this.setState({
        showWarningPopup: true,
        pendingAction: { type: "checkboxValue", args: { field, key, value } }
      });
    } else {
      this.onCheckBoxValueChange(field, key, value);
    }
  }

  onCheckBoxValueChange = (field, key, value) => {
    let newFilters = this.state.more_filters;
    let filterField = getKeyForFilter(field);
    const updatedMissingFields = buildMissingFieldsQuery(newFilters?.missing_fields || {}, filterField, key, value);
    newFilters = {
      ...newFilters,
      missing_fields: updatedMissingFields
    };
    this.updateFiltersAndReload(newFilters);
  };

  getFiltersState(state) {
    let moreFiltersKeys = state.more_filters;
    if (state.more_filters && Object.keys(state.more_filters).length) {
      moreFiltersKeys = Object.keys(state.more_filters).reduce((acc, item) => {
        const filterConfig = this.columns.find(column => column.filterField === item);
        if (filterConfig && filterConfig.filterType === "apiMultiSelect") {
          acc[item] = state.more_filters[item].map(filter => filter.key || filter);
          return acc;
        }
        if (filterConfig && filterConfig.filterType === "cascade") {
          let label = {};
          state.more_filters[item].forEach(l => {
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
        acc[item] = state.more_filters[item];
        return acc;
      }, {});
    }

    // sort state ( user clicked) takes priority over default sort prop
    let sort =
      Object.keys(state.sort_order).length > 0
        ? Object.keys(state.sort_order).map(key => state.sort_order[key])
        : this.props.sort || [];
    let filter = { ...moreFiltersKeys, ...this.state.search_parsed, partial: { ...this.state.partial_parsed } };

    if (this.props.isLeadTimeByStage && typeof sort?.[0]?.id !== "string") {
      sort = [{ id: this?.props?.activeColumn, desc: true }];
    }
    filter = removeEmptyKeys(filter);

    const teams = get(filter, ["teams"], undefined);
    const code_area = get(filter, ["code_area"], undefined);
    if (teams || code_area) {
      if (teams) {
        unset(filter, ["teams"]);
      }

      if (code_area) {
        unset(filter, ["code_area"]);
      }
    }

    let finalFilters = this.mapFiltersBeforeCall({
      ...this.state.filters,
      page: state.page - 1 || 0,
      page_size: state.pageSize,
      //sort: state.sorted || this.props.sort || [],
      sort: sort,
      [this.props.filterKey || "filter"]: {
        ...filter
      },
      across: this.props.across || "",
      interval: this.props.interval,
      ...(this.props.ouFilters || {})
    });

    if (this.props.transformFilters) {
      finalFilters = this.props.transformFilters(finalFilters);
    }

    // Adding `widget_id` key to the payload; needed for BE ES
    if (this.props?.widgetId) {
      finalFilters["widget_id"] = this.props.widgetId;
    }
    if (this.props?.widget) {
      finalFilters["widget"] = this.props.widget;
    }

    return finalFilters;
  }

  clearFilters() {
    // include filters from more filters that are not a part of the filters
    // this comes from the calling component and must be preserved
    const filters = this.getFiltersConfig();
    let remainingFilters = {};
    Object.keys(this.props.moreFilters).forEach(field => {
      if (filters.filter(f => f.field === field).length === 0) {
        remainingFilters[field] = this.props.moreFilters[field];
      }
    });
    this.setState(
      {
        more_filters: { ...remainingFilters },
        partial_parsed: {}
      },
      () => {
        if (this.props.onFiltersChange) {
          this.props.onFiltersChange(this.state.more_filters);
        }
        this.fetchData(this.state);
      }
    );
  }

  isClearFilters() {
    const hasDelete = this.props.rowSelection?.selectedRowKeys?.length > 0;
    if (hasDelete) {
      this.setState({ showWarningPopup: true, pendingAction: { type: "clearFilterButtonPressed", data: null } });
    } else {
      this.clearFilters();
    }
  }

  checkForAppliedFilters() {
    if (this.state.more_filters && Object.keys(this.state.more_filters).length) {
      return true;
    }

    const partialFiltersKeys = Object.keys(this.state.partial_parsed);
    const partialFilters = partialFiltersKeys.find(key => this.state.partial_parsed[key]);
    return !!partialFilters;
  }

  getCountForAppliedFilters() {
    if (this.state.more_filters) {
      const { more_filters } = this.state;
      const presentColumns = this.props.allColumns || this.columns;
      let totalKeys = [
        ...Object.keys(more_filters),
        ...Object.keys(get(more_filters, ["exclude"], {})),
        ...Object.keys(get(more_filters, ["partial_match"], {}))
      ];
      const columnsWithPrefixPath = presentColumns.filter(column => column.prefixPath !== undefined);
      columnsWithPrefixPath.forEach(col => {
        totalKeys = [
          ...totalKeys,
          ...Object.keys(get(more_filters, [col.prefixPath], {})),
          ...Object.keys(get(more_filters, ["exclude", col.prefixPath], {}))
        ];
      });
      return uniq(totalKeys).filter(
        filter =>
          presentColumns.map(column => column.filterField || "nothing").includes(filter) &&
          more_filters &&
          hasFilterValue(more_filters, filter)
      ).length;
    }
    return 0;
  }

  getDynamicColumnJsxHeaders() {
    let jsxHeaders = [];
    this.visibleColumns.forEach(col => {
      if (isValidElement(col?.title) && !col?.hidden) {
        let jsxTitle = col?.titleForCSV;
        jsxHeaders.push({
          title: jsxTitle ? jsxTitle : capitalize(col?.dataIndex?.replace(/_/g, " ")),
          key: col?.dataIndex
        });
      }
    });
    return jsxHeaders;
  }

  handleCSVDownload() {
    const _columns = this.visibleColumns.filter(col => !isValidElement(col.title) && col.title !== "Actions");
    const _jsxHeader = [...this.getDynamicColumnJsxHeaders()];
    this.props.csvDownload(this.props.uri, this.props.method, {
      transformer: this.props.downloadCSV?.tableDataTransformer,
      filters: this.getFiltersState(this.state),
      columns: _columns,
      jsxHeaders: _jsxHeader,
      derive: this.props.derive,
      shouldDerive: this.props.shouldDerive === "all" ? [] : this.props.shouldDerive
    });
  }

  onSortChange(pagination, filters, sorter, extra) {
    let sort_field = sorter.field;
    if (sort_field?.includes(dynamicColumnPrefix)) {
      sort_field = sort_field.replace(dynamicColumnPrefix, "");
    }
    // check for existing sort entry sorter.order, sorter.field
    const sortEntry = { id: sort_field, desc: sorter.order === "descend" };
    //let entries = this.state.sort_order;
    // * ADD this key (API_CALL_ON_TABLE_SORT_CHANGE) in column config if there is no need to call the API
    const currentColumn = (this.columns || []).find(column => column.key === sorter["columnKey"]);
    const fetchDataWithNewSort = get(currentColumn, [API_CALL_ON_TABLE_SORT_CHANGE], true) !== false;
    if (fetchDataWithNewSort) {
      let entries = {};
      entries[sort_field] = sortEntry;

      if (this.props.uri === "lead_time_values" && sort_field !== "data" && this.state.activeColumn !== sort_field) {
        this.setState(
          {
            activeColumn: sort_field,
            sort_order: entries
          },
          () => this.fetchData(this.state)
        );
      } else {
        this.setState({ sort_order: entries }, () => this.fetchData(this.state));
      }
    }
  }

  get dynamicColumns() {
    if (!this.props.hasDynamicColumns) {
      return [];
    }
    const records = this.filteredPaginationData;
    if (!records || records.length === 0) {
      return [];
    }
    let dynamicColumns = [];
    if (this.props.renderDynamicColumns) {
      if (
        ["lead_time_values", "lead_time_for_change_drilldown", "mean_time_to_restore_drilldown"].includes(
          this.props.uri
        ) ||
        [LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT, DORA_REPORTS.LEADTIME_CHANGES].includes(
          this.props.report
        )
      ) {
        const dynamicColumnKeys = Object.keys(records[0]).filter(key => key.includes("dynamic_column_"));
        dynamicColumnKeys.forEach((dynamicColumn, i) => {
          dynamicColumns.push(this.props.renderDynamicColumns(dynamicColumn, this.state.activeColumn));
        });
      } else {
        records.forEach(record => {
          const dynamicColumnKeys = Object.keys(record).filter(key => key.includes("dynamic_column_"));
          dynamicColumnKeys.forEach((dynamicColumn, i) => {
            const alreadyExists = dynamicColumns.find(column => {
              return column.key === dynamicColumn;
            });
            if (!alreadyExists) {
              dynamicColumns.push(this.props.renderDynamicColumns(dynamicColumn));
            }
          });
          dynamicColumns = orderBy(dynamicColumns, ["key"], ["desc"]);
        });
      }
    }
    if (this.props.configureDynamicColumns) {
      dynamicColumns = this.props.configureDynamicColumns(dynamicColumns);
    }
    return dynamicColumns;
  }

  get defaultColumns() {
    const { columns } = this.props;
    return [...columns, ...this.dynamicColumns];
  }

  get columns() {
    const { selectedDrilldownColumns, report } = this.props;
    if (!selectedDrilldownColumns) {
      return this.defaultColumns;
    }

    let allColumns = this.props.allColumns ?? get(widgetConstants, [report, "drilldown", "columns"], []);

    if ([...Object.values(DORA_REPORTS), ...Object.values(LEAD_MTTR_DORA_REPORTS), TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT, TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT].includes(this.props.report)) {
      const drilldownColumnTransformFunction = get(
        widgetConstants,
        [this.props.report, "drilldown", "drilldownColumnTransformer"],
        []
      );

      if (drilldownColumnTransformFunction) {
        let categoryColorMapping;
        allColumns = drilldownColumnTransformFunction({
          columns: allColumns,
          categoryColorMapping,
          filters: {
            integrationType: this.props.doraProfileIntegrationType || "",
            integrationApplication: this.props.doraProfileIntegrationApplication || "",
            getAllColumn: true,
            query: this.props.moreFilters,
          },
          doraProfileDeploymentRoute: this.props.doraProfileDeploymentRoute,
          doraProfileEvent: this.props.doraProfileEvent
        });
      }
    }

    let getCustomColumn = get(widgetConstants, [this.props.report, "drilldown", "getCustomColumn"], []);
    if ([TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT].includes(this.props.report) && getCustomColumn) {
      let customFiled = getCustomColumn(this.props.testrailsCustomField);
      allColumns = [...allColumns, ...customFiled];
    }

    const savedColumns = selectedDrilldownColumns
      .map(
        drilldownColumn =>
          this.defaultColumns.find(column => column.dataIndex === drilldownColumn) ||
          allColumns.find(column => column.dataIndex === drilldownColumn)
      )
      .filter(column => column);
    if (!this.isLoading && (savedColumns.length === 0 || savedColumns.length !== selectedDrilldownColumns.length)) {
      this.props.resetDrilldownColumns(this.props.widgetId);
      this.props.setSelectedColumns(undefined);
    }
    return savedColumns;
  }

  getRecordFields() {
    if (this.props.drilldown) {
      let drilldownColumns = get(widgetConstants, [this.props.report, "drilldown", "columns"], []);

      if ([...Object.values(DORA_REPORTS), ...Object.values(LEAD_MTTR_DORA_REPORTS), TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT,  TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT].includes(this.props.report)) {
        const drilldownColumnTransformFunction = get(
          widgetConstants,
          [this.props.report, "drilldown", "drilldownColumnTransformer"],
          []
        );

        if (drilldownColumnTransformFunction) {
          let categoryColorMapping;
          drilldownColumns = drilldownColumnTransformFunction({
            columns: drilldownColumns,
            categoryColorMapping,
            filters: {
              integrationType: this.props.doraProfileIntegrationType || "",
              integrationApplication: this.props.doraProfileIntegrationApplication || "",
              getAllColumn: true,
              query: this.props.moreFilters,
            },
            doraProfileDeploymentRoute: this.props.doraProfileDeploymentRoute,
            doraProfileEvent: this.props.doraProfileEvent
          });
        }
      }

      let getCustomColumn = get(widgetConstants, [this.props.report, "drilldown", "getCustomColumn"], []);
      if ([TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT].includes(this.props.report) && getCustomColumn) {
        let customFiled = getCustomColumn(this.props.testrailsCustomField);
        drilldownColumns = [...drilldownColumns, ...customFiled];
      }

      return [...drilldownColumns, ...this.dynamicColumns]
        .filter(column => !column.hidden)
        .map(column => {
          let title = typeof column.title === "string" ? column.title : column.titleForCSV;
          if (!title) {
            title = toTitleCase(column.dataIndex);
          }
          return {
            title,
            dataIndex: column.dataIndex
          };
        });
    }
    return [];
  }

  get acrossBasedColumn() {
    const allColumns = clone(this.props.columns);
    let mappedColumn = find(allColumns, column => !!column.hasAcrossBasedTitle);
    let column = undefined;
    if (mappedColumn) {
      const { paginationState } = this.props;
      const loading = paginationState.loading !== undefined ? paginationState.loading : true;
      const data = this.filteredPaginationData;
      if (!loading && data.length) {
        const across = this.getFiltersState(this.state).across;
        const mapping = data[0][mappedColumn.key] || [];
        const acrossColumn = find(mapping, m => m.key === across);
        if (acrossColumn) {
          column = { ...mappedColumn, title: acrossColumn.name || mappedColumn.title };
        }
      }
    }
    return column;
  }

  get visibleColumns() {
    const sortingColumn = sortingColumnForFixedLeft(this.columns);
    const fColumns = sortingColumn.filter(c => !c.hidden);

    const acrossColumn = this.acrossBasedColumn;
    if (acrossColumn) {
      const aIndex = findIndex(fColumns, c => c.key === acrossColumn.key);
      if (aIndex !== -1) {
        const filteredColumns = filter(fColumns, c => c.key !== acrossColumn.key);
        const temp = filteredColumns[aIndex];
        filteredColumns[aIndex] = acrossColumn;
        if (temp) {
          filteredColumns.splice(aIndex + 1, 0, temp);
        }
        return filteredColumns;
      }
      return fColumns;
    }
    if (this.props.expandedRowRender) {
      fColumns.forEach(_col => delete _col.width);
    }
    return fColumns;
  }

  popupOnCancel() {
    this.setState(state => {
      if (state.showDeletePopup) {
        return { showDeletePopup: false };
      } else {
        return { showWarningPopup: false, pendingAction: {} };
      }
    });
  }

  popupOnOk() {
    if (this.state.showDeletePopup) {
      // this.setState({ bulkDeleteLoading: true });
      this.props.onBulkDelete();
    } else {
      this.setState(
        {
          showWarningPopup: false
        },
        () => {
          this.props?.clearSelectedIds && this.props.clearSelectedIds();
          this.doPendingAction();
        }
      );
    }
  }

  doPendingAction() {
    const { pendingAction } = this.state;
    if (Object.keys(pendingAction).length > 0 && pendingAction.hasOwnProperty("type")) {
      switch (pendingAction.type) {
        case "generalSearch":
          this.setState(
            {
              ...pendingAction.data,
              pendingAction: {}
            },
            () => this.getPaginationData()
          );
          break;
        case "clearFilterButtonPressed":
          this.clearFilters();
          break;
        case "optionSelect":
          {
            const { field, option, type } = pendingAction.args;
            this.onOptionSelectHandler(field, option, type);
          }

          break;
        case "inputChange":
          {
            const { field, value } = pendingAction.args;
            this.onInputChange(field, value);
          }
          break;
        case "excludeSwitch":
          {
            const { field, value } = pendingAction.args;
            this.onExcludeSwitchChange(field, value);
          }

          break;
        case "checkboxValue":
          {
            const { field, key, value } = pendingAction.args;
            this.onCheckBoxValueChange(field, key, value);
          }
          break;
        case "binary":
          {
            const { field, value } = pendingAction.args;
            this.onBinaryChange(field, value);
          }
          break;
        case "partial_match":
          this.setState(
            {
              ...pendingAction.data,
              pendingAction: {}
            },
            () => this.getPaginationData()
          );
        default:
      }
    }
  }

  onRemoveFilter(key) {
    const prefixPath = this.getPrefixPath(key);
    const _filters = removeFilterKey(this.state.more_filters, key, false, prefixPath);
    this.setState(
      {
        more_filters: _filters
      },
      () => this.debouncedPaginationGet()
    );
  }

  memoizedErrorDesc = memoizeOne(() => getServerErrorDesc(ServerErrorSource.SERVER_PAGINATED_TABLE));

  render() {
    const totalPages = this.props.pagination_total
      ? Math.ceil(this.props.pagination_total / this.state.pageSize) * 2
      : 1;
    const filtersConfig = this.getFiltersConfig();
    const { paginationState, showSelectionCount, clearSelectedIds, searchURI, paginatedSearchState } = this.props;
    let loading = true,
      data = {};

    if (this.state.partial_parsed && this.state.partial_parsed[this.props.generalSearchField] && searchURI) {
      loading = get(paginatedSearchState, ["loading"], true);
      data = get(paginatedSearchState, ["data"], {});
    } else {
      loading = paginationState.loading !== undefined ? paginationState.loading : true;
      data = paginationState.data || {};
    }
    const finalLoading = this.visibleColumns.length !== 0 && !loading ? false : true;

    const metadata = data._metadata || {};
    const paginationCondition = !finalLoading;
    const selectedRowsCount = this.props.rowSelection?.selectedRowKeys?.length || 0;
    const showCustomChanger = !finalLoading && metadata.total_count < this.state.pageSize && !this.props.drilldown;
    const showVersion = this.props.uri === "org_users";
    const version = get(this.props.queryParam, "version", undefined);

    let titleProps = {
      title: (
        <div className="flex align-center flex-wrap">
          <div className="flex direction-column align-items-start">
            <div className="flex align-center">
              <div style={{ marginRight: ".3rem" }}>{this.props.title || "Results"} </div>
              {this.props.hasTitleSearch && (
                <AntInput
                  id={`${this.props.generalSearchField}-search`}
                  placeholder={this.props.searchPlaceholder || "Search ... "}
                  type="search"
                  onChange={this.onGeneralSearchHandler}
                  name="general-search"
                  className={this.props.searchClassName || ""}
                  value={(this.state.partial_parsed && this.state.partial_parsed[this.props.generalSearchField]) || ""}
                />
              )}
              <div style={{ marginRight: ".3rem" }}>
                {!finalLoading && this.props.displayCount && (
                  <Badge
                    style={{ backgroundColor: "var(--harness-blue)" }}
                    count={metadata.total_count || 0}
                    overflowCount={metadata.total_count || 0}
                  />
                )}
              </div>
              {selectedRowsCount > 0 && showSelectionCount && (
                <div style={{ marginRight: ".3rem" }} className="flex align-center">
                  <Tag style={{ color: "#51acff", backgroundColor: "#eaf8ff", borderColor: "#51acff", marginRight: 0 }}>
                    {" "}
                    {selectedRowsCount} Selected
                  </Tag>
                  {clearSelectedIds && (
                    <AntButton type="link" onClick={() => clearSelectedIds()}>
                      Clear Selection
                    </AntButton>
                  )}
                </div>
              )}
              <div>{this.props?.drilldownCheckbox}</div>
            </div>
            {showVersion && !!version && (
              <div style={{ display: "flex", fontSize: "14px", fontWeight: 400, color: "#8C8C8C" }}>
                {`Showing Version ${version}`}
                {typeof this.props?.showNewVersion === "function" && (
                  <a
                    style={{ marginLeft: ".5rem", alignItems: "center", gap: ".3rem", color: "var(--harness-blue)" }}
                    className="flex"
                    onClick={this.props?.showNewVersion}>
                    <Icon type="edit" />
                    <span>Change Version</span>
                  </a>
                )}
              </div>
            )}
          </div>
        </div>
      )
    };

    if (this.props.componentTitle) {
      titleProps = { title: this.props.componentTitle };
    }

    if (!this.props.showTitle) {
      delete titleProps.title;
    }

    const configFilterKeys = filtersConfig.map(config => config.field);
    const includeMissingFields = get(widgetConstants, [this.props.report, "includeMissingFieldsInPreview"], false);
    const globalFilters = Object.keys(this.state.more_filters).reduce((acc, next) => {
      if (
        (!["or", "jira_or", "product_id"].includes(next) && configFilterKeys.includes(next)) ||
        ["custom_fields", "exclude", "workitem_attributes", "time_range"].includes(next) ||
        (includeMissingFields && ["missing_fields"].includes(next))
      ) {
        return { ...acc, [next]: this.state.more_filters[next] };
      } else {
        return acc;
      }
    }, {});

    const splitButtonFilter = {
      jiraOrFilters: get(this.state.more_filters, ["or"], {}) || get(this.state.more_filters, ["jira_or"], {}),
      globalFilters: { alltypes: globalFilters }
    };

    const _morefilters = Object.keys(this.state.more_filters || {}).reduce((acc, next) => {
      if (next === "workitem_attributes") {
        const keys = Object.keys(this.state.more_filters[next] || {});
        let values = {};
        keys.forEach(key => {
          values = { ...values, [key]: this.state.more_filters[next][key] };
        });
        return { ...acc, ...values };
      }
      return { ...acc, [next]: this.state.more_filters[next] };
    }, {});

    let extraProps = {
      extra:
        this.props.uuid !== DRILLDOWN_UUID
          ? !(this.props.drilldown && finalLoading) && (
              <ExtraFilterContent
                hasDelete={this.props.hasDelete}
                hasSearch={this.props.hasSearch}
                hasFilters={this.props.hasFilters}
                hasAppliedFilters={this.checkForAppliedFilters()}
                generalSearchField={this.props.generalSearchField}
                onGeneralSearchHandler={this.onGeneralSearchHandler}
                countForAppliedFilters={this.getCountForAppliedFilters()}
                onToggleFilters={() => this.onToggleFilters()}
                downloadCSV={this.props.downloadCSV}
                clearFilters={this.isClearFilters}
                handleCSVDownload={this.handleCSVDownload}
                setShowDeletePopup={this.deleteButtonhandler}
                selectedRows={this.props.rowSelection?.selectedRowKeys?.length || 0}
                partialParsed={this.state.partial_parsed}
                filters={
                  this.props.uri === "org_users"
                    ? { ...this.state.more_filters, ...this.state.partial_parsed }
                    : splitButtonFilter
                }
                integrationIds={this.state.more_filters.integration_ids}
                showFiltersDropDown={this.state.showFiltersDropDown}
                setShowFiltersDropDown={val => this.setShowFiltersDropDown(val)}
                showCustomFilters={this.props.showCustomFilters || this.props.uri === "org_users"}
                showUsersFilters={this.props.uri === "org_users"}
                filtersConfig={filtersConfig}
                queryParam={this.props.queryParam}
                customExtraContent={this.props.customExtraContent}
                extraSuffixActionButtons={this.props.extraSuffixActionButtons}
                searchPlaceholder={this.props.searchPlaceholder}
                reportType={this.props.report}
                availableColumns={this.getDrillDownColumnsList()}
                visibleColumns={this.visibleColumns}
                widgetId={this.props.widgetId}
                defaultColumns={this.defaultColumns}
                setSelectedColumns={this.props.setSelectedColumns}
                hideFilterButton={this.props.hideFilterButton}
                showOnlyFilterIcon={this.props.showOnlyFilterIcon}
                newSearch={this.props.newSearch}
              />
          )
          : !(this.props.drilldown && finalLoading) && (
              <DrillDownFilterContent
                downloadCSV={this.props.downloadCSV}
                handleCSVDownload={this.handleCSVDownload}
                drilldownHeaderProps={this.props.drilldownHeaderProps}
                displayColumnSelector={{
                  availableColumns: this.getRecordFields(),
                  visibleColumns: this.visibleColumns,
                  widgetId: this.props.widgetId,
                  defaultColumns: this.defaultColumns.filter(column => !column.hidden)
                }}
                setSelectedColumns={this.props.setSelectedColumns}
              />
          )
    };

    let filters = (
      <Filters
        key={"filters"}
        filtersConfig={filtersConfig}
        onSearchEvent={this.onSearchHandler}
        onOptionSelectEvent={this.onOptionSelectHandlerWrapper}
        onInputChange={this.onInputChangeWrapper}
        onTagsChange={this.onTagsChange}
        onExcludeSwitchChange={this.onExcludeSwitchChangeWrapper}
        onCheckBoxValueChange={this.onCheckBoxValueChangeWrapper}
        onBinaryChange={this.onBinaryChangeWrapper}
        more_filters={_morefilters}
        onCloseFilters={this.onToggleFilters}
        onInputFieldChangeHandler={this.onInputFieldChangeHandlerWrapper}
      />
    );

    if (this.props.showCustomFilters) {
      filters = (
        <CustomFiltersContainer
          onCloseFilters={this.onToggleFilters}
          key={"filters"}
          filtersConfig={filtersConfig}
          onSearchEvent={this.onSearchHandler}
          onOptionSelectEvent={this.onOptionSelectHandlerWrapper}
          onInputChange={this.onInputChangeWrapper}
          onTagsChange={this.onTagsChange}
          onExcludeSwitchChange={this.onExcludeSwitchChangeWrapper}
          onCheckBoxValueChange={this.onCheckBoxValueChangeWrapper}
          onBinaryChange={this.onBinaryChangeWrapper}
          more_filters={_morefilters}
          countForAppliedFilters={this.getCountForAppliedFilters()}
          onRemoveFilter={this.onRemoveFilter}
        />
      );
    } else if (this.props.uri === "org_users" || this.props.uri === "organization_unit_management") {
      filters = (
        <NewTableFiltersContainer
          key="filters"
          filtersConfig={filtersConfig}
          onSearchEvent={this.onSearchHandler}
          onOptionSelectEvent={this.onOptionSelectHandlerWrapper}
          onInputChange={this.onInputChangeWrapper}
          onTagsChange={this.onTagsChange}
          onExcludeSwitchChange={this.onExcludeSwitchChangeWrapper}
          onCloseFilters={this.onToggleFilters}
          onBinaryChange={this.onBinaryChangeWrapper}
          hideSaveBtn
        />
      );
    }

    if (!this.props.showExtra) {
      delete extraProps.extra;
    }

    const serverError = this.props.rest_api[this.props.uri]?.[this.props.method]?.[this.props.uuid];
    const serverErrorDesc = this.memoizedErrorDesc();

    if (serverError && serverError?.error_code >= ERROR_CODE_RANGE_START) {
      return <EmptyApiErrorWidget description={serverErrorDesc} className={"api-error"}></EmptyApiErrorWidget>;
    }

    return (
      <Card
        data-testid="server-paginated-table"
        {...titleProps}
        bordered={false}
        {...extraProps}
        // bodyStyle={{ padding: "2px 0px 30px" }}
        headStyle={this.props.drilldown ? { paddingRight: "0" } : {}}>
        <div className={(this.props.className, cx({ "remove-border": this.props.drilldown && finalLoading }))}>
          {!!this.state.filtersDisplayed && filters}
          <PaginatedSelectPopup
            onCancel={this.popupOnCancel}
            onOk={this.popupOnOk}
            warningVisible={this.state.showWarningPopup}
            deleteVisible={this.state.showDeletePopup}
            selectedItemCount={this.props.rowSelection?.selectedRowKeys?.length || 0}
            bulkDeleting={this.props.bulkDeleting}
          />
          <AntTable
            hasCustomPagination={paginationCondition}
            dataSource={finalLoading ? [] : this.filteredPaginationData}
            expandedRowRender={this.props.expandedRowRender}
            expandedRowKeys={[this.state.expanded_row_keys]}
            onExpand={this.onExpandRowHandler}
            columns={finalLoading && this.props.drilldown ? [] : this.visibleColumns}
            onPageChange={this.onPageChangeHandler}
            onPageSizeChange={this.onPageSizeChangeHandler}
            pageSize={this.state.pageSize}
            page={this.state.page}
            onRow={this.props.onRow}
            className={this.props.className}
            locale={{
              emptyText: finalLoading ? (
                <div className="rest-table-spinner">
                  <Spin />
                </div>
              ) : (
                ""
              )
            }}
            rowSelection={this.props.rowSelection}
            totalPages={totalPages}
            totalRecords={metadata.total_count || 0}
            size={"middle"}
            rowKey={this.props.rowKey}
            onChange={this.onSortChange}
            bordered={this.props.bordered}
            scroll={this.props.scroll}
            rowClassName={this.props.rowClassName}
            pagination={false}
            hasPagination={this.props.hasPagination}
            showCustomChanger={showCustomChanger}
            drilldownFooter={this.props.drilldownFooter}
          />
        </div>
        {/* {this.props.drilldownFooter && !finalLoading && this.props.drilldownFooter} */}
      </Card>
    );
  }
}

ServerPaginatedTable.propTypes = {
  displayCount: PropTypes.bool,
  useFilters: PropTypes.bool,
  dataFilter: PropTypes.object,
  method: PropTypes.string,
  uri: PropTypes.string,
  hasFilters: PropTypes.bool,
  hasSort: PropTypes.bool,
  hasSearch: PropTypes.bool,
  showTitle: PropTypes.bool,
  showExtra: PropTypes.bool,
  title: PropTypes.string,
  generalSearchField: PropTypes.string,
  partialFilters: PropTypes.object,
  moreFilters: PropTypes.object,
  pageName: PropTypes.string,
  pageSize: PropTypes.number,
  reload: PropTypes.number.isRequired,
  uuid: PropTypes.any,
  onFiltersChanged: PropTypes.func,
  onFiltersChange: PropTypes.func,
  derive: PropTypes.bool.isRequired,
  bordered: PropTypes.bool,
  shouldDerive: PropTypes.any,
  componentTitle: PropTypes.any,
  hasDynamicColumns: PropTypes.bool,
  renderDynamicColumns: PropTypes.func,
  configureDynamicColumns: PropTypes.func,
  columns: PropTypes.any,
  scroll: PropTypes.any,
  className: PropTypes.string,
  rowClassName: PropTypes.func,
  defaultExpandedRowKeys: PropTypes.number,
  onBulkDelete: PropTypes.func,
  hasDelete: PropTypes.bool,
  bulkDeleting: PropTypes.bool,
  showSelectionCount: PropTypes.bool,
  clearSelectedIds: PropTypes.any,
  showCustomFilters: PropTypes.bool,
  report: PropTypes.string,
  queryParam: PropTypes.any,
  drilldownHeaderProps: PropTypes.any,
  customExtraContent: PropTypes.any,
  activeColumn: PropTypes.any,
  // These buttons are added after the custom table buttons like delete and filters etc.
  extraSuffixActionButtons: PropTypes.element,
  searchPlaceholder: PropTypes.string,
  filterKey: PropTypes.string,
  ouFilters: PropTypes.any,
  transformRecordsData: PropTypes.func,
  interval: PropTypes.string,
  drilldown: PropTypes.bool,
  skipConfirmationDialog: PropTypes.bool,
  hasTitleSearch: PropTypes.bool,
  searchClassName: PropTypes.string,
  searchURI: PropTypes.string,
  transformFilters: PropTypes.func,
  clearFiltersCount: PropTypes.number,
  drilldownFooter: PropTypes.element,
  generalSearchPartialKey: PropTypes.string,
  bulkDeleteRestriction: PropTypes.bool,
  hideFilterButton: PropTypes.bool,
  doraProfileIntegrationType: PropTypes.string,
  doraProfileDeploymentRoute: PropTypes.string,
  doraProfileIntegrationApplication: PropTypes.string,
  testrailsCustomField: PropTypes.any,
  widgetMetaData: PropTypes.any
};

ServerPaginatedTable.defaultProps = {
  rowKey: "id",
  displayCount: true,
  useFilters: false,
  dataFilter: { field: "id", values: [] },
  method: "list",
  hasFilters: true,
  hasSort: false,
  hasSearch: true,
  showTitle: true,
  showExtra: true,
  title: "",
  filterKey: "filter",
  generalSearchField: "name",
  partialFilters: {},
  moreFilters: {},
  pageName: "default",
  pageSize: DEFAULT_PAGE_SIZE,
  reload: 1,
  uuid: "0",
  derive: true,
  shouldDerive: "all",
  componentTitle: undefined,
  onFiltersChanged: () => {},
  scroll: {},
  rowClassName: () => {},
  defaultExpandedRowKeys: undefined,
  onBulkDelete: () => {},
  hasDelete: false,
  bulkDeleting: false,
  showSelectionCount: true,
  className: "server-paginated-table-container",
  report: "",
  queryParam: {},
  customExtraContent: null,
  activeColumn: undefined,
  searchPlaceholder: "Search ...",
  ouFilters: undefined,
  interval: undefined,
  drilldown: false,
  skipConfirmationDialog: false,
  clearFiltersCount: 0,
  drilldownFooter: undefined,
  generalSearchPartialKey: undefined,
  bulkDeleteRestriction: false,
  hideFilterButton: false,
  doraProfileIntegrationType: "",
  doraProfileDeploymentRoute: "pr",
  doraProfileIntegrationApplication: "",
  showNewVersion: undefined,
  setSearchValue: undefined,
  testrailsCustomField: [],
  widgetMetaData: ""
};

const mapStateToProps = (state, ownProps) => ({
  ...mapPaginationStatetoProps(state),
  page_settings: getPageSettingsSelector(state),
  paginationState: genericPaginationData(state, ownProps),
  ...mapRestapiStatetoProps(state),
  paginatedSearchState: genericPaginationSeachData(state, ownProps)
});

const mapDispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPaginationDispatchtoProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch),
  csvDownload: (uri, method, data) => dispatch(genericTableCSVDownload(uri, method, data)),
  resetDrilldownColumns: widgetId => dispatch(widgetDrilldownColumnsUpdate(widgetId, undefined))
});

export default ErrorWrapper(connect(mapStateToProps, mapDispatchToProps)(ServerPaginatedTable));
