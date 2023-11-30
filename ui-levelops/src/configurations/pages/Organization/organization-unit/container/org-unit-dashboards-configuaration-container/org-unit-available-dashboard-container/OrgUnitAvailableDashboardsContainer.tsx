import React, { useEffect, useMemo, useRef, useState } from "react";
import queryString from "query-string";
import { ColumnProps } from "antd/lib/table";
import { OU_DASHBOARDS_LIST_ID } from "configurations/pages/Organization/Constants";
import { get, debounce, isEqual } from "lodash";
import { useDispatch } from "react-redux";
import { dashboardsList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntCard, AntIcon, AntInput, AntTable, AntText } from "shared-resources/components";
import { useLocation } from "react-router-dom";
import { sanitizeObjectCompletely } from "utils/commonUtils";
interface OrgUnitAvailableDashboardsProps {
  rowSelection: any;
  columns: Array<ColumnProps<any>>;
  title: string;
}
const OrgUnitAvailableDashboardsContainer: React.FC<OrgUnitAvailableDashboardsProps> = (
  props: OrgUnitAvailableDashboardsProps
) => {
  const { rowSelection, columns, title } = props;
  const location = useLocation();
  const { ou_workspace_id } = queryString.parse(location.search);
  const [list, setList] = useState<any[]>([]);
  const [dashboardListLoading, setDashboardListLoading] = useState<boolean>(false);
  const [searchField, setSearchField] = useState<string>("");
  const [totalRecords, setTotalRecords] = useState<number>(0);
  const [pageConfig, setPageConfig] = useState<{ page: number; page_size: number }>({
    page: 1,
    page_size: 15
  });
  const dispatch = useDispatch();
  const prevPageRef = useRef<number>(pageConfig.page);

  const dashboardListState = useParamSelector(getGenericUUIDSelector, {
    uri: "dashboards",
    method: "list",
    uuid: OU_DASHBOARDS_LIST_ID
  });

  const fetchDashboardList = (value?: string) => {
    if (!dashboardListLoading) {
      setDashboardListLoading(true);
      dispatch(
        dashboardsList(
          {
            page: pageConfig.page - 1,
            page_size: pageConfig.page_size,
            filter: sanitizeObjectCompletely({
              partial: { name: value || "" },
              workspace_id: !!ou_workspace_id ? parseInt(ou_workspace_id as string) : ""
            })
          },
          OU_DASHBOARDS_LIST_ID
        )
      );
    }
  };

  useEffect(() => {
    fetchDashboardList();
  }, []);

  useEffect(() => {
    if (!isEqual(prevPageRef.current, pageConfig.page)) {
      prevPageRef.current = pageConfig.page;
      fetchDashboardList();
    }
  }, [pageConfig.page]);

  useEffect(() => {
    if (dashboardListLoading) {
      const loading = get(dashboardListState, ["loading"], true);
      const error = get(dashboardListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records = get(dashboardListState, ["data", "records"], []);
          const totalCount = get(dashboardListState, ["data", "_metadata", "total_count"], 0);
          setTotalRecords(totalCount);
          setList(records);
          dispatch(restapiClear("dashboards", "list", OU_DASHBOARDS_LIST_ID));
        }
        setDashboardListLoading(false);
      }
    }
  }, [dashboardListState, dashboardListLoading]);

  const debouncedFetchDashboards = debounce((search: string) => {
    fetchDashboardList(search);
  }, 100);

  const debouncedFetchDashboardsRef = useRef(debouncedFetchDashboards);

  const handleSearchChange = (value: string) => {
    setSearchField(value);
    debouncedFetchDashboardsRef.current(value);
  };

  const onPageChangeHandler = (page: number) => {
    setPageConfig(prev => ({
      ...prev,
      page: page
    }));
  };

  const renderOUDashboardCardTitle = useMemo(
    () => (
      <div className="flex">
        <AntText>{`${title} (${totalRecords})`}</AntText>
      </div>
    ),
    [totalRecords]
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
        dataSource={list}
        columns={columns}
        onPageChange={onPageChangeHandler}
        pageSize={pageConfig.page_size}
        page={pageConfig.page}
        loading={dashboardListLoading}
        showPageSizeOptions={false}
        rowSelection={rowSelection}
        totalRecords={totalRecords}
        paginationSize={"small"}
        hideTotal={true}
        size={"middle"}
        rowKey={"id"}
      />
    </AntCard>
  );
};

export default OrgUnitAvailableDashboardsContainer;
