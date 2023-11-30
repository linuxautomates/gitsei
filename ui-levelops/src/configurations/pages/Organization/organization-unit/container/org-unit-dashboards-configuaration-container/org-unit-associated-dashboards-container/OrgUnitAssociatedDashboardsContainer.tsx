import React, { useMemo, useState } from "react";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { orgUnitBasicInfoType, OUDashboardType } from "configurations/configuration-types/OUTypes";
import { title } from "process";
import { AntCard, AntIcon, AntInput, AntTable, AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import OrgUnitAssociatedDashboardActionRow from "./OrgUnitAssociatedDashboardActionRow";
import { uniqBy } from "lodash";

interface OrgUnitAssociatedDashboardsProps {
  orgUnit: RestOrganizationUnit;
  rowSelection: any;
  handleOUChanges: (key: orgUnitBasicInfoType, value: any) => void;
  associatedDashboards: Array<OUDashboardType>;
  inheritedDashboards: Array<OUDashboardType>;
  dashboardListLoading: boolean;
}
const OrgUnitAssociatedDashboardsContainer: React.FC<OrgUnitAssociatedDashboardsProps> = (
  props: OrgUnitAssociatedDashboardsProps
) => {
  const {
    orgUnit,
    rowSelection,
    associatedDashboards,
    inheritedDashboards,
    dashboardListLoading,
    handleOUChanges
  } = props;
  const [searchField, setSearchField] = useState<string>("");
  const [pageConfig, setPageConfig] = useState<{ page: number; page_size: number }>({
    page: 1,
    page_size: 15
  });

  const handleSearchChange = (value: string) => {
    setSearchField(value);
  };

  const onPageChangeHandler = (page: number) => {
    setPageConfig(prev => ({
      ...prev,
      page: page
    }));
  };

  const getAssociatedDashboardColumns = useMemo(
    () => [
      {
        ...baseColumnConfig("Name", "name", { width: "90%" }),
        render: (item: string, rec: any) => (
          <OrgUnitAssociatedDashboardActionRow
            curDashboards={rec}
            associatedDashboards={orgUnit.dashboards}
            isDefault={rec.dashboard_id === orgUnit?.defaultDashboardId?.toString()}
            isInherited={!!inheritedDashboards.find(d => d.dashboard_id === rec.dashboard_id)}
            handleOUChanges={handleOUChanges}
          />
        )
      }
    ],
    [orgUnit.dashboards, orgUnit.defaultDashboardId, inheritedDashboards]
  );

  const getDatasource = useMemo(() => {
    return uniqBy(
      [...inheritedDashboards, ...associatedDashboards].filter(dash =>
        dash?.name?.toLowerCase().includes(searchField?.toLowerCase())
      ),
      "dashboard_id"
    );
  }, [associatedDashboards, inheritedDashboards, searchField, pageConfig]);

  const getFinalDataSource = useMemo(() => {
    let high = pageConfig.page_size * pageConfig.page;
    let low = high - pageConfig.page_size + 1;
    if (pageConfig.page > 1 && low > getDatasource?.length) {
      high -= pageConfig.page_size;
      low = high - pageConfig.page_size + 1;
      setPageConfig(prev => ({
        ...prev,
        page: prev.page - 1
      }));
    }
    return getDatasource?.slice(low - 1, high);
  }, [pageConfig, getDatasource]);

  const renderOUDashboardCardTitle = useMemo(
    () => (
      <div className="flex">
        <AntText>{`Associated Insights (${getDatasource.length})`}</AntText>
      </div>
    ),
    [getDatasource]
  );

  return (
    <AntCard className="ou-dashboard-association-table" title={renderOUDashboardCardTitle} key={title}>
      <AntInput
        placeholder="Search here"
        onChange={(e: any) => handleSearchChange(e.target.value)}
        className="search-box"
        value={searchField}
        prefix={<AntIcon type="search" />}
      />
      <AntTable
        hasCustomPagination={!dashboardListLoading}
        dataSource={getFinalDataSource}
        columns={getAssociatedDashboardColumns}
        onPageChange={onPageChangeHandler}
        pageSize={pageConfig.page_size}
        page={pageConfig.page}
        loading={dashboardListLoading}
        showPageSizeOptions={false}
        rowSelection={rowSelection}
        totalRecords={getDatasource.length}
        paginationSize={"small"}
        hideTotal={true}
        size={"middle"}
        rowKey={"dashboard_id"}
      />
    </AntCard>
  );
};

export default OrgUnitAssociatedDashboardsContainer;
