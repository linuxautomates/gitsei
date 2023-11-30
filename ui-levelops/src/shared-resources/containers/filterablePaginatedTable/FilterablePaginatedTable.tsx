import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntPopoverComponent as AntPopover } from "shared-resources/components/ant-popover/ant-popover.component";
import { AntTableComponent as AntTable } from "shared-resources/components/ant-table/ant-table.component";
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
import { PaginatedTableProps } from "../server-paginated-table/containers/table/paginated-table";
import ColumnFilterDropdown from "./ColumnFilterDropdown";
import { SortOptions } from "./constants";
import cx from "classnames";
import "./FilterablePaginatedTable.scss";
import { filterData, getIconName, sortData } from "./helper";
import { Badge, Empty } from "antd";
import { usePagination } from "custom-hooks/usePagination";
import ConfigureFilterableTableBodyCell from "./ConfigureFilterableTableBodyCell";
import { RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import SortFilterDisplay from "./SortFiltersDisplay";
import { FilterableContext } from "./FilterableContext";
import NewLegendComponent from "shared-resources/charts/jira-effort-allocation-chart/components/LegendComponent/EffortInvestmentLegend";
import {
  engineerRatingTypeNew,
  ratingKeyMapping,
  ratingToLegendColorMappingNew
} from "dashboard/constants/devProductivity.constant";
import { cloneDeep } from "lodash";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";

export interface FilterablePaginatedTableProps extends PaginatedTableProps {
  dataSource: any[];
  className?: any;
  columns: any;
  onClick?: any;
  id?: any;
  dashboardOuIds?: any;
  tableFilters: Map<string, { key: string; value: string }>;
  saveTableFilters: (filters: Map<string, { key: string; value: string }>) => void;
  isDemo?: boolean;
  query?: any;
  widgetEntitlements?: any;
}

const FilterablePaginatedTable = (props: FilterablePaginatedTableProps) => {
  const {
    dataSource,
    columns,
    onClick,
    id,
    dashboardOuIds,
    tableFilters,
    saveTableFilters,
    isDemo,
    query,
    widgetEntitlements
  } = props;

  const [sortList, setSortList] = useState<{ index: string; order: SortOptions; isNumeric: boolean } | null>(null);
  const [filteredDataSource, setFilteredDataSource] = useState<Array<any>>(cloneDeep(dataSource));
  const [dropdownIndex, setDropdownIndex] = useState<string | undefined>(undefined);
  const [pageSize, setPageSize] = useState<number>(10);
  const [totalCount, page, setPage, currentPageData, totalPages] = usePagination(filteredDataSource, pageSize);
  const widgetQuery: RestWidget = useParamSelector(getWidget, { widget_id: id });
  const newTrellisProfile = widgetEntitlements?.["TRELLIS_BY_JOB_ROLES"]; // useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  const [legendFilters, setLegendFilters] = useState<Record<string, any>>(
    Object.keys(ratingToLegendColorMappingNew).reduce(
      (acc: any, filter: string) => ({ ...acc, [filter]: newTrellisProfile }),
      {}
    )
  );

  const components = {
    body: {
      cell: ConfigureFilterableTableBodyCell
    }
  };

  const setSorting = (index: string, order: SortOptions, isNumeric: boolean = false) => {
    setSortList({ index, order, isNumeric });
    setDropdownIndex(undefined);
  };

  const resetSort = (index: string) => {
    if (index === sortList?.index) {
      setSortList(null);
    }
  };
  const updatedData = useCallback(
    (newData: any) => {
      let newDataSource = cloneDeep(newData);
      newDataSource = (newDataSource || []).map((data: any, index: number) => {
        const keys = Object.keys(data);
        keys.filter(key => {
          if (key.includes("_color")) {
            if (legendFilters[data[key]] || (data[key] === "NO_COLOR" && legendFilters[dataSource[index][key]])) {
              data[key] = dataSource[index][key];
            } else {
              data[key] = "NO_COLOR";
            }
          }
        });
        data.no_score_legend_check = legendFilters?.NO_SCORE;
        data.no_color = !newTrellisProfile;
        return data;
      });
      return newDataSource;
    },
    [tableFilters, dataSource, legendFilters, newTrellisProfile]
  );

  useEffect(() => {
    let filteredData: any = filterData(dataSource, tableFilters);
    filteredData = updatedData(filteredData);
    if (filteredData && filteredData.length) {
      setFilteredDataSource(sortData(filteredData, sortList));
    } else {
      setFilteredDataSource(filteredData);
    }
  }, [tableFilters, dataSource, legendFilters, newTrellisProfile]);

  useEffect(() => {
    if (sortList && filteredDataSource && filteredDataSource.length) {
      setFilteredDataSource(sortData(filteredDataSource, sortList));
    }
  }, [sortList]);

  const updateFilters = (
    filters: Map<
      string,
      {
        key: string;
        value: string;
      }
    >
  ) => {
    saveTableFilters(filters);
    setDropdownIndex(undefined);
  };

  const filterDropdown = (dataIndex: string, isNumericColumn: boolean) => (
    <ColumnFilterDropdown
      key={dataIndex}
      index={dataIndex}
      sortList={sortList}
      setSort={setSorting}
      filters={tableFilters}
      setFilters={updateFilters}
      isNumeric={isNumericColumn}
      resetSort={resetSort}
    />
  );

  const handleDropdownVisibleChange = (visible: boolean) => {
    if (!visible) {
      setDropdownIndex(undefined);
    }
  };

  const getTitle = (column: any) => {
    const columnIndex = column.dataIndex;
    const isNumericColumn = column.isNumeric;
    const sortOrder = sortList?.index === columnIndex ? sortList?.order : undefined;
    const isFilterAvailable = !!tableFilters.get(columnIndex);

    if (columnIndex === "full_name" || columnIndex === "name") {
      return (
        <div className="d-inline-block align-center">
          {column.title}
          <Badge overflowCount={1000} count={totalCount || 0} className="count-badge" />
        </div>
      );
    }
    return (
      <div className={cx({ filteredColumn: isFilterAvailable || !!sortOrder })}>
        {column.title}
        {!isDemo && (
          <AntPopover
            content={filterDropdown(columnIndex, isNumericColumn)}
            trigger={"click"}
            visible={!!dropdownIndex && dropdownIndex === columnIndex}
            onVisibleChange={handleDropdownVisibleChange}>
            <AntButton className={"ant-btn-outline columnFilterButton"} onClick={() => setDropdownIndex(columnIndex)}>
              <SvgIcon className="reports-btn-icon" icon={getIconName(isFilterAvailable, sortOrder)} />
            </AntButton>
          </AntPopover>
        )}
      </div>
    );
  };

  const showPagination = useMemo(() => {
    return !props.loading;
  }, [props.loading]);

  const visibleColumns = columns?.map((column: any) => {
    if (column.children) {
      const col = {
        title: column.title,
        align: "left",
        children: [
          ...column.children.map((childColumn: any) => ({
            ...childColumn,
            title: getTitle(childColumn)
          }))
        ]
      };
      return col;
    }
    return {
      ...column,
      align: "left",
      title: getTitle(column)
    };
  });

  if (!dataSource || dataSource.length <= 0) {
    return <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  const onCellClick = (record: any, columnName: string) => {
    const interval = widgetQuery?.query?.interval;
    if (isDemo && onClick) {
      onClick({
        widgetId: id,
        record,
        columnName,
        name: record?.full_name,
        interval: query?.interval
      });
      return;
    }
    if (onClick) {
      onClick({
        record,
        columnName,
        interval,
        dashboardOuIds
      });
    }
  };

  const resetFilters = () => {
    saveTableFilters(new Map());
    setSortList(null);
  };

  return (
    <>
      <FilterableContext.Provider value={{ onCellClick }}>
        {!isDemo && <SortFilterDisplay sortList={sortList} filters={tableFilters} resetFilters={resetFilters} />}
        <AntTable
          hasCustomPagination={showPagination}
          rowClassName={() => "filterable-table-row"}
          className={"filter-table-container"}
          dataSource={currentPageData}
          scroll={{ x: "max-content" }}
          bordered={true}
          pageSize={pageSize}
          page={page}
          onPageChange={setPage}
          columns={visibleColumns}
          totalRecords={totalCount}
          totalPages={totalPages}
          showPageSizeOptions={false}
          components={components}
        />
      </FilterableContext.Provider>
      {newTrellisProfile && (
        <NewLegendComponent
          filters={legendFilters}
          setFilters={setLegendFilters}
          colors={ratingToLegendColorMappingNew}
        />
      )}
    </>
  );
};

export default FilterablePaginatedTable;
