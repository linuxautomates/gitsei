import React, { useCallback, useState } from "react";

import RestApiPaginatedTable, { RestApiPaginatedTableProps } from "./rest-api-paginated-table";

interface RestApiPaginatedTableWrapperProps extends RestApiPaginatedTableProps {
  onFiltersChange?: (filters: any) => void;
}

const RestApiPaginatedTableStateWrapper: React.FC<RestApiPaginatedTableWrapperProps> = (
  props: RestApiPaginatedTableWrapperProps
) => {
  const [filters, setFilters] = useState({});
  const [page, setPage] = useState(1);

  const { onFiltersChange } = props;

  const handleFiltersChange = useCallback((filters: any) => {
    setPage(1);
    setFilters(filters);
    onFiltersChange && onFiltersChange(filters);
  }, []);

  const handlePageChange = useCallback((page: number) => {
    setPage(page);
  }, []);

  return (
    <RestApiPaginatedTable
      page={page}
      onPageChange={handlePageChange}
      onFiltersChange={handleFiltersChange}
      filters={filters}
    />
  );
};

export default RestApiPaginatedTableStateWrapper;
