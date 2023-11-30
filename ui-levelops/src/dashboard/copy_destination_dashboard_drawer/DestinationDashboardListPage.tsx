import React from "react";
import queryString from "query-string";
import { useSelector } from "react-redux";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import RestApiPaginatedTable from "shared-resources/containers/server-paginated-table/rest-api-paginated-table";
import { useLocation } from "react-router-dom";

interface DestinationDashboardListPageProps {
  page: number;
  searchQuery: string;
  rowClassName: (record: any, index: number) => string;
  handleSearchChange: (searchQuery: string) => void;
  handlePageChange: (page: number) => void;
  handleRowClick: (id: string, rowIndex: number) => void;
}

const DestinationDashboardListPage: React.FC<DestinationDashboardListPageProps> = ({
  handleRowClick,
  handlePageChange,
  handleSearchChange,
  rowClassName,
  searchQuery,
  page
}) => {
  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const location = useLocation();
  const { workspace_id } = queryString.parse(location.search);
  return (
    <div className="destination-dashboards-list-container">
      <RestApiPaginatedTable
        generalSearchField="name"
        title="Insights"
        restCall="dashboardsList"
        uri="dashboards"
        pageSize={10}
        searchQuery={searchQuery}
        onSearchChange={handleSearchChange}
        bordered={true}
        page={page}
        rowClassName={rowClassName}
        onPageChange={handlePageChange}
        filters={{
          workspace_id: !!workspace_id || !!selectedWorkspace ? parseInt(workspace_id ?? selectedWorkspace?.id) : ""
        }}
        hasFilters={false}
        columns={[{ title: "", dataIndex: "name", key: "name" }]}
        uuid={"destination_dashboards-list"}
        showPageSizeOptions={false}
        onRow={(record: any, rowIndex: number) => {
          return {
            onClick: (e: any) => handleRowClick(record?.id || "", rowIndex)
          };
        }}
      />
    </div>
  );
};

export default DestinationDashboardListPage;
