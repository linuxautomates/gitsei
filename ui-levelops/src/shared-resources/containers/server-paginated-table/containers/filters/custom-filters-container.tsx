import React, { useEffect, useState } from "react";
import { Icon, Tabs } from "antd";
import FiltersViewContainer from "./filters-view-container";
import "./custom.filters.container.scss";
import DashboardFilterComponent from "./dashboard-filters/dashboaed-filter-component";
import { filterCount } from "../../../../../dashboard/components/dashboard-header/helper";
import queryString from "query-string";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useDispatch } from "react-redux";
import { get, intersection } from "lodash";
import { genericList } from "reduxConfigs/actions/restapi";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import Loader from "components/Loader/Loader";
import { OR_QUERY_APPLICATIONS } from "dashboard/pages/dashboard-drill-down-preview/helper";

interface CustomFiltersContainer {
  onCloseFilters: () => void;
  filtersConfig: any;
  onSearchEvent: (field: any, event: any) => void;
  onOptionSelectEvent: (field: any, value: any, type?: string) => void;
  onInputChange: (field: any, value: string) => void;
  onBinaryChange: (field: any, event: any) => void;
  onTagsChange: (type: string, value: any) => void;
  onMultiOptionsChangeEvent: () => void;
  onExcludeSwitchChange: (field: string, value: any) => void;
  onCheckBoxValueChange: (field: string, key: any, value: any) => void;
  more_filters: any;
  countForAppliedFilters: number;
  onRemoveFilter: (field: string) => void;
}

const { TabPane } = Tabs;
const CustomFiltersContainer: React.FC<CustomFiltersContainer> = props => {
  const [activeKey, setActiveKey] = useState<string>("filters");
  const [application, setApplication] = useState<string>("");
  const [dashboardId, setDashboardId] = useState<string>("");

  useEffect(() => {
    setApplication(queryString.parse(window.location.href.split("?")[1]).application as string);
    setDashboardId(queryString.parse(window.location.href).dashboardId as string);
  }, []);

  return (
    <div className={"custom-filters-container"}>
      <Icon type="close" style={{ right: "1rem", position: "absolute", zIndex: 1 }} onClick={props.onCloseFilters} />
      <Tabs activeKey={activeKey} animated={false} size={"small"} onChange={(key: string) => setActiveKey(key)}>
        <TabPane key={"filters"} tab={`Filters (${props.countForAppliedFilters})`}>
          <div style={{ padding: "1.5rem" }}>
            <FiltersViewContainer
              filtersConfig={props.filtersConfig}
              onSearchEvent={props.onSearchEvent}
              onOptionSelectEvent={props.onOptionSelectEvent}
              onInputChange={props.onInputChange}
              onBinaryChange={props.onBinaryChange}
              onTagsChange={props.onTagsChange}
              onMultiOptionsChangeEvent={props.onMultiOptionsChangeEvent}
              more_filters={props.more_filters}
              onExcludeSwitchChange={props.onExcludeSwitchChange}
              onCheckBoxValueChange={props.onCheckBoxValueChange}
              onRemoveFilter={props.onRemoveFilter}
            />
          </div>
        </TabPane>
        {OR_QUERY_APPLICATIONS.filter((key: string) => application.includes(key)).length > 0 && (
          <TabPane key={"dashboard_filters"} tab={`Dashboard Filters (${filterCount(props.more_filters.or || {})})`}>
            <DashboardFilterComponent dashboardId={dashboardId} />
          </TabPane>
        )}
      </Tabs>
    </div>
  );
};

export default CustomFiltersContainer;
