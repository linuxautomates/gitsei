import { AutoComplete, Empty, Icon } from "antd";
import * as React from "react";
import { AntIcon, AntInput } from "shared-resources/components";
import { useCallback, useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dashboardsList } from "reduxConfigs/actions/restapi";
import { debounce, get } from "lodash";
import { DASHBOARD_ROUTES, getBaseUrl } from "../../../constants/routePaths";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";

const DASH_SEARCH_BAR = "DASH_SERACH_BAR";

interface DashboardSearchInputProps {
  history: any;
  searchPlaceholder?: string;
}

export const DashboardSearchInput: React.FC<DashboardSearchInputProps> = props => {
  const dispatch = useDispatch();

  const [searchValue, setSearchValue] = useState<string | undefined>(undefined);
  const [listLoading, setListLoading] = useState(false);
  const [list, setList] = useState<any[]>([]);

  const projectParams = useParams<ProjectPathProps>();

  const dashboardListState = useSelector(state => {
    return get(state, ["restapiReducer", "dashboards", "list", DASH_SEARCH_BAR], { loading: true, error: true });
  });

  const dashboardListSearch = (value?: string) => {
    setListLoading(true);
    dispatch(dashboardsList({ page_size: 1000, filter: { partial: { name: value } } }, DASH_SEARCH_BAR));
  };

  const debouncedSearch = useCallback(
    debounce((v: string) => {
      dashboardListSearch(v);
      setSearchValue(v);
    }, 250),
    [] // eslint-disable-line react-hooks/exhaustive-deps
  );

  useEffect(() => {
    if (listLoading || searchValue || get(dashboardListState, ["data", "records"], []).length !== list.length) {
      const { loading, error } = dashboardListState;
      if (loading !== undefined && !loading && error !== undefined && !error) {
        const data = dashboardListState.data.records;
        setList(data);
        setListLoading(false);
      }
    }
  }, [dashboardListState]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    // @ts-ignore
    <AutoComplete
      className={"dashboard-search-container"}
      id="dashboard-search-input-autocomplete"
      dataSource={list.map((item: any) => ({ value: item.id, text: item.name }))}
      onSearch={(value: string) => debouncedSearch(value)}
      notFoundContent={searchValue && !listLoading && <Empty />}
      onSelect={(id: any) => props.history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}/${id}`)}>
      <AntInput
        type="search"
        id="dashboard-search-input"
        placeholder={props.searchPlaceholder || "Search..."}
        name="search-input"
        autoComplete="off"
      />
    </AutoComplete>
  );
};
