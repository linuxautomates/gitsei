import React, { useEffect, useState, useMemo } from "react";
import { AntListComponent as AntList } from "../ant-list/ant-list.component";
import { default as AntListItem } from "../ant-list-item/AntListItem";
import InfiniteScroll from "react-infinite-scroller";
import { useDebounce } from "custom-hooks/useDebounce";
import "./FilterList.style.scss";
import { SELECT_NAME_TYPE_ITERATION } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
interface FilterListProps {
  dataSource: any[];
  dataKey: string;
  header?: React.ReactNode;
  labelKey?: string;
  tableHeader: string;
}

const PAGE_SIZE = 50;

const FilterList: React.FC<FilterListProps> = props => {
  const { dataSource, dataKey, header, labelKey } = props;

  const [currentPage, setCurrentPage] = useState<number>(1);
  const debouncedCurrentPage = useDebounce(currentPage, 200);

  useEffect(() => {
    setCurrentPage(1);
  }, dataSource);

  const handleLoadMore = (page: number) => {
    setCurrentPage(page);
  };

  const localDataSource = useMemo(() => {
    return dataSource.slice(0, PAGE_SIZE * debouncedCurrentPage);
  }, [debouncedCurrentPage]);

  const hasMore = localDataSource.length < dataSource.length && debouncedCurrentPage === currentPage;

  return (
    <div className="filter-list">
      <AntList header={!!header && header}>
        <div className="filter-list__scroll-parent">
          <InfiniteScroll
            initialLoad={false}
            pageStart={1}
            loadMore={handleLoadMore}
            hasMore={hasMore}
            useWindow={false}>
            {localDataSource.map(item => {
              const itemValue = item?.[dataKey] || "";
              let label = itemValue;
              if (labelKey) {
                label = item?.[labelKey] || "";
              }
              label = props.tableHeader?.toLowerCase() === SELECT_NAME_TYPE_ITERATION ? item?.key : label;
              return !!itemValue && <AntListItem key={itemValue}>{label}</AntListItem>;
            })}
          </InfiniteScroll>
        </div>
      </AntList>
    </div>
  );
};

FilterList.defaultProps = {
  dataSource: [],
  dataKey: "key"
};

export default FilterList;
