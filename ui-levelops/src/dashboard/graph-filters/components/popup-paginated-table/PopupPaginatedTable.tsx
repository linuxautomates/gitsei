import { AutoComplete, Icon } from "antd";
import classNames from "classnames";
import { useDebounce } from "custom-hooks/useDebounce";
import React, { useCallback, useMemo, useState } from "react";
import {
  AntBadge,
  AntButton,
  AntInput,
  AntRadioGroup,
  AntTable,
  AntText,
  CustomFormItemLabel
} from "shared-resources/components";

export interface PopupPaginatedTableProps {
  filterValueLoading?: boolean;
  dataSource: any[];
  tableHeader?: string;
  tableMiddle?: boolean;
  tableMiddleProps?: {
    selectedOnPageJobsCount: (pageFilter: any, filteredData: any[]) => number;
    selectPageHandler: (pageFiltersContent: any, filteredData: any[], selectedRowsKeys: any[]) => void;
    clearSelection: () => void;
    allSelectionHandler: (filteredData: any[]) => void;
    titleName: string;
  };
  columns: any[];
  valueKey: string;
  createOption?: boolean | undefined;
  isCustom?: boolean | undefined;

  // If an element in the dataSource looks like
  // { label: "hello", value: "123" }
  // you should provide labelKey = "label"
  // so that it is used for display on the table.
  // otherwise, valueKey is fallback.
  labelKey?: string;

  // Limits user to one selected row.
  singleSelect?: boolean;
  onSelectionChange: (...args: any) => any;
  selectedRowsKeys: any[];
  addbuttonHandler?: (value: string) => void;
  noAddCustomValue?: boolean;
  tableHeaderRadioButton?: boolean;
  tableHeaderRadioButtonProps?: {
    renderSelectionOptions?: any;
    handleSelectdAllJobFlag?: (value: string) => void;
    selectdAllJobFlag?: string;
    otherTabSelectionType?: string;
    renderJobSortingOptions?: any;
  };
  newMenuOption?: boolean
}

const DEFAULT_PAGE_SIZE = 10;

const PopupPaginatedTable: React.FC<PopupPaginatedTableProps> = props => {
  const { selectedRowsKeys, noAddCustomValue, newMenuOption=false } = props;
  const [searchValue, setSearchValue] = useState<string>("");
  const [pageFilters, setPageFilters] = useState<any>({ page: 1, pageSize: DEFAULT_PAGE_SIZE });
  const debouncedSearchValue = useDebounce(searchValue, 500);

  const onSelectChange = useCallback((selectedRowKeys: any[], selectedRows: any[]) => {
    props.onSelectionChange && props.onSelectionChange(selectedRowKeys);
  }, [props.onSelectionChange]);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedRowsKeys,
      onChange: onSelectChange,
      columnWidth: "2%",
      type: props.singleSelect ? "radio" : "checkbox"
    }),
    [selectedRowsKeys, onSelectChange, props.singleSelect]
  );

  const handlePageChange = useCallback((page: number) => {
    setPageFilters({ pageSize: pageFilters.pageSize || DEFAULT_PAGE_SIZE, page });
  }, [pageFilters.pageSize]);

  const filteredData =
    useMemo(() => {
      let curData = [...(props.dataSource || [])];
      if (debouncedSearchValue?.length) {
        curData = curData.filter(item =>
          item?.[props.labelKey || props.valueKey || "key"]
            ?.toLowerCase?.()
            ?.includes(debouncedSearchValue?.toLowerCase())
        );
      }
      handlePageChange(1);
      return curData;
    }, [debouncedSearchValue, props.dataSource, props.labelKey, props.valueKey]) || [];

  const totalPagination = {
    pageSize: pageFilters.pageSize || DEFAULT_PAGE_SIZE,
    current: pageFilters.page || 1,
    onChange: handlePageChange,
    onShowSizeChange: (currentPage: number, newPageSize: number) => {
      setPageFilters({ pageSize: newPageSize, page: 1 });
    },
    showTotal: (total: number, range: any[]) => `${range[0]}-${range[1]} of ${total}`,
    total: filteredData.length || 0,
    size: "small",
    showSizeChanger: true,
    pageSizeOptions: ["10", "20", "50", "100"]
  };

  const notFoundContent = () => {
    return (
      <div className={"custom-select-container__no-content-found-div"}>
        <div className="custom-select-container__no-content-found-div__label-div">{`"${searchValue}" not found`}</div>
        <AntButton
          type="primary"
          className="custom-select-container__no-content-found-div__button"
          onClick={() => {
            props.addbuttonHandler && props.addbuttonHandler(searchValue);
            setSearchValue("");
          }}>
          Add Value
        </AntButton>
      </div>
    );
  };

  const onRadioChange = useCallback((value: string) => {
    if (props?.tableHeaderRadioButtonProps?.handleSelectdAllJobFlag)
      props?.tableHeaderRadioButtonProps?.handleSelectdAllJobFlag(value)
  },[props?.tableHeaderRadioButtonProps?.handleSelectdAllJobFlag]);

  const tableHeader = useMemo(() => {
    return props.tableHeader && typeof props.tableHeader === "string"
      ? <CustomFormItemLabel label={props.tableHeader} />
      : props.tableHeader
  }, [props.tableHeader]);

  const tableHeaderRadioButton = useMemo(() => {
    return (
      props.tableHeaderRadioButton &&
      <>
        <AntRadioGroup value={props?.tableHeaderRadioButtonProps?.selectdAllJobFlag} onChange={(e: any) => onRadioChange(e.target.value)} className="radio-button-filed">
          <div className="stage-description-wrapper-form-end-event-options">
            {props?.tableHeaderRadioButtonProps?.renderSelectionOptions}
          </div>
        </AntRadioGroup>
        {props?.tableHeaderRadioButtonProps?.selectdAllJobFlag !== 'ALL' &&
          <div className="job-filter-class">
            <span className="job-filter-dropdown flex">
              <span className="job-filter-title">Show by:</span> {props?.tableHeaderRadioButtonProps?.renderJobSortingOptions}
            </span>
          </div>
        }
      </>
    )
  }, [props.tableHeaderRadioButton, props?.tableHeaderRadioButtonProps, onRadioChange]);

  const tableHeaderWarning = useMemo(() => {
    return (props.tableHeaderRadioButton && props?.tableHeaderRadioButtonProps?.otherTabSelectionType && props?.tableHeaderRadioButtonProps?.otherTabSelectionType !== props?.tableHeaderRadioButtonProps?.selectdAllJobFlag &&
      <div className="job-selection-warning">
        <p><Icon type="warning" className="icon-style" theme="twoTone" twoToneColor="rgb(223, 165, 42)" />  Warning: Job Selection Conflict!</p>
        <p>
          Conflict detected in selection criteria for deployments causing failure and total deployments. Options:
          <br></br>
          1. Choose Include all jobs for both or
          <br></br>
          2. Choose Select jobs manually for both.
        </p>
      </div>
    )
  }, [props.tableHeaderRadioButton, props?.tableHeaderRadioButtonProps?.otherTabSelectionType, props?.tableHeaderRadioButtonProps?.selectdAllJobFlag]);

  const tableHeaderSearching = useMemo(() => {
    return (
      <div className="mb-10">
        <AutoComplete
          dataSource={[]}
          notFoundContent={!!searchValue
            ? filteredData.length || props.selectedRowsKeys.includes(searchValue)
              ? ""
              : noAddCustomValue ? "" : notFoundContent()
            : ""}
          placeholder="Search"
          value={searchValue}
          filterOption={true}
          style={{ width: "50%" }}
          onSearch={(value: string) => {
            setSearchValue(value);
          }}>
          <AntInput className="search-field" type="search" />
        </AutoComplete>
      </div>
    )
  }, [searchValue, filteredData, props.selectedRowsKeys, noAddCustomValue, notFoundContent, setSearchValue]);

  const tableHeaderRadioButtonOption = useMemo(() => {
    return (props.tableHeaderRadioButton &&
      <div
        className="flex justify-space-between align-center"
        style={{
          margin: "0.5rem 0rem 0.8rem"
        }}>
        {<div style={{ display: "flex", flexDirection: "row" }}>
          <AntText onClick={() => props?.tableMiddleProps?.allSelectionHandler(filteredData)} class="job-lable job-lable-blue">{`Select all ${props?.tableMiddleProps?.titleName}`}</AntText>
          <AntBadge
            overflowCount={filteredData.length}
            count={filteredData.length}
            className={classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}
            style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }} />
          <AntText onClick={() => props?.tableMiddleProps?.selectPageHandler(pageFilters, filteredData, selectedRowsKeys)} class="job-lable job-lable-blue job-lable-padding">{`Select ${props?.tableMiddleProps?.titleName} on this page`}</AntText>
          <AntBadge
            overflowCount={props?.tableMiddleProps?.selectedOnPageJobsCount(pageFilters, filteredData)}
            count={props?.tableMiddleProps?.selectedOnPageJobsCount(pageFilters, filteredData)}
            className={classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}
            style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }} />
          <AntText onClick={props?.tableMiddleProps?.clearSelection} class="job-lable job-lable-black job-lable-padding">{`Clear selection`}</AntText>
        </div>}
      </div>
    )
  }, [props.tableHeaderRadioButton, props?.tableMiddleProps, filteredData, pageFilters, selectedRowsKeys]);

  const newMenuOptions = useMemo(() => {
    return (
      props.tableHeaderRadioButton && (
        <div
          className="flex justify-space-between align-center"
          style={{
            margin: "0.5rem 0rem 0.8rem"
          }}>
          {
            <div style={{ display: "flex", flexDirection: "row" }}>
              <AntText
                onClick={() => props?.tableMiddleProps?.allSelectionHandler(filteredData)}
                class="job-lable job-lable-blue">{`Select all ${props?.tableMiddleProps?.titleName}`}</AntText>
              <AntBadge
                count={selectedRowsKeys?.length}
                className={classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}
                style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }}
              />
              <AntText
                onClick={() => props?.tableMiddleProps?.selectPageHandler(pageFilters, filteredData, selectedRowsKeys)}
                class="job-lable job-lable-blue job-lable-padding">{`Select ${props?.tableMiddleProps?.titleName} on this page`}</AntText>
              <AntText
                onClick={props?.tableMiddleProps?.clearSelection}
                class="job-lable job-lable-black job-lable-padding">{`Clear selection`}</AntText>
            </div>
          }
        </div>
      )
    );
  }, [
    props.tableHeaderRadioButton,
    props?.tableMiddleProps,
    filteredData,
    pageFilters,
    selectedRowsKeys,
    newMenuOption
  ]);
  const tableHeaderForManuallyJob = useMemo(() => {
    return (
      props?.tableHeaderRadioButtonProps?.selectdAllJobFlag !== "ALL" && (
        <>
          {tableHeaderSearching}
          {newMenuOption ? newMenuOptions : tableHeaderRadioButtonOption}
        </>
      )
    );
  }, [
    props?.tableHeaderRadioButtonProps?.selectdAllJobFlag,
    tableHeaderSearching,
    tableHeaderRadioButtonOption,
    newMenuOption
  ]);

  return (
    <div className="flex direction-column popup-paginated-table" style={{ width: "33vw" }}>
      <div
        className="flex justify-space-between align-center"
        style={{
          margin: "0.5rem 0rem 0.8rem"
        }}>
        {tableHeader}
        {tableHeaderRadioButton}
      </div>
      {tableHeaderWarning}
      {tableHeaderForManuallyJob}
      <AntTable
        rowKey={(row: any) => row[props.valueKey]}
        hasCustomPagination={false}
        pagination={totalPagination}
        dataSource={filteredData || []}
        loading={props.filterValueLoading}
        columns={props.columns}
        rowSelection={props?.tableHeaderRadioButtonProps?.selectdAllJobFlag !== "ALL" ? rowSelection : ""}
      />
    </div>
  );
};

export default PopupPaginatedTable;
