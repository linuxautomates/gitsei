import { notification, Tabs } from "antd";
import React, { useCallback, useEffect, useState, useMemo } from "react";
import LocalStoreService from "services/localStoreService";
import { WorkitemList } from "workitems/containers";
import { tabs } from "./constants";
import queryString from "query-string";
import { RouteComponentProps } from "react-router-dom";
import { mapFiltersToIds, mapNonApiFilters, mapUserIdsToAssigneeOrReporter } from "dashboard/helpers/helper";
import { useDispatch, useSelector } from "react-redux";
import { restapiClear, workItemBulkDelete } from "reduxConfigs/actions/restapi";
import { formClear } from "reduxConfigs/actions/formActions";
import { workbenchTabCounts } from "reduxConfigs/actions/tabCountActions";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { v1 as uuid } from "uuid";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import Loader from "components/Loader/Loader";
import { getTabCountState } from "reduxConfigs/selectors/tabCountSelector";
import { get } from "lodash";
import { getformState } from "reduxConfigs/selectors/formSelector";
import { buildQueryParamsFromObject, parseQueryParamsIntoKeys } from "utils/queryUtils";
import { queryParamsFromFilters, queryParamsToParse } from "workitems/helpers/workitem-parsed-query.handler";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import moment from "moment";
import { DATE_RANGE_FILTER_FORMAT } from "../../shared-resources/containers/server-paginated-table/containers/filters/constants";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  DEFAULT_FILTER_UUID,
  getIssueFilter,
  ISSUES_UUID,
  listIssueFilter,
  updateIssueFilter
} from "reduxConfigs/actions/restapi/issueFilter.actions";
import { TriageFilterDropDownList } from "../../model/triage/triage";
import { IssueFilterDropDown } from "../components/issue-filter-dropdown/IssueFilterDropDown";
import { sanitizeTimeFilter } from "../components/issue-filter-dropdown/helper";
import { WebRoutes } from "../../routes/WebRoutes";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { USERROLES } from "routes/helper/constants";
import { BASE_UI_URL } from "helper/envPath.helper";
import { getWorkitemDetailPage } from "constants/routePaths";

const { TabPane } = Tabs;

const WorkItemListPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const { history, location } = props;

  const [activeTab, setActiveTab] = useState<string>("all");
  const [tabCountLoading, setTabCountLoading] = useState<boolean>(true);
  const [tabCounts, setTabCounts] = useState<any[]>([]);
  const [filterFromLocation, setFilterFromLocation] = useState({});
  const [mappedApiFilters, setMappedApiFilters] = useState<any>({});
  const [formName, setFormName] = useState<string>("");
  const [filtersLoading, setFiltersLoading] = useState<boolean>(false);
  const [selectedIds, setSelectedIds] = useState<any[]>([]);
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState<number>(1);

  const [selectedFilter, setSelectedFilter] = useState<string | undefined>(undefined);
  const [defaultFiltersLoading, setDefaultFiltersLoading] = useState(true);
  const [loadingSavedFilters, setLoadingSavedFilters] = useState(true);
  const [filterDescription, setFilterDescription] = useState<string>("");
  const [isDefaultFilter, setIsDefaultFilter] = useState<boolean>(false);
  const [noDefaultFilterFound, setNoDefaultFilterFound] = useState<boolean>(false);
  const [createFilterWithData, setCreateFilterWithData] = useState<any>({});
  const [updateInitialFilters, setUpdateInitialFilters] = useState<boolean>(false);
  const [savingFilters, setSavingFilters] = useState<boolean>(false);

  const dispatch = useDispatch();
  const tabCountState = useSelector(getTabCountState);
  const curFormState = useSelector((state: any) => getformState(state, formName));
  const workItemBulkDeleteState = useSelector(state => get(state, ["restapiReducer", "workitem", "bulkDelete"], {}));
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const ls = new LocalStoreService();

  const filteredTabs = useMemo(() => {
    const userRbac = ls.getUserRbac()?.toLowerCase();
    if (userRbac) {
      return tabs.filter((tab: any) => tab.rbac.includes(userRbac as USERROLES));
    }
    return tabs;
  }, []);

  const filterDefaultListState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "list",
    uuid: DEFAULT_FILTER_UUID
  });

  const filterListState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "list",
    uuid: "0"
  });

  const filterSaveState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "update",
    uuid: selectedFilter
  });

  const selectedFilterState = useParamSelector(getGenericRestAPISelector, {
    uri: "issue_filters",
    method: "get",
    uuid: selectedFilter
  });

  const getFilterDataFromId = (id: string) => {
    const list = get(filterListState, ["data", "records"], []);
    const filterItem = list.find((item: any) => item.name === id);
    return filterItem?.filter || {};
  };

  useEffect(() => {
    if (isTrialUser) {
      // @ts-ignore
      props.history.push({ pathname: WebRoutes.dashboard.details(props.match.params, "") });
    }
  }, [isTrialUser]);

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    if (values.filters) {
      setFilterFromLocation(values.filters ? JSON.parse(values.filters as string) : {});
    } else if (values.filter_name) {
      const filterName = values.filter_name as string;
      setSelectedFilter(filterName);
      setDefaultFiltersLoading(false);
      setLoadingSavedFilters(true);
      dispatch(getIssueFilter(filterName));
    } else {
      const existingData = get(filterDefaultListState, ["data", "records", 0, "filter"], {});
      setLoadingSavedFilters(false);
      if (!Object.keys(existingData).length) {
        dispatch(listIssueFilter({ filter: { is_default: true } }, DEFAULT_FILTER_UUID));
      } else {
        setDefaultFiltersLoading(false);
      }
    }
    const filters = filteredTabs
      .map(tab => tab.filters)
      .map(filter => {
        if (filter.hasOwnProperty("assignee_user_ids")) {
          filter.assignee_user_ids = [ls.getUserId()];
        }
        if (filter.hasOwnProperty("reporter")) {
          filter.reporter = ls.getUserEmail();
        }
        return { ...filter };
      });
    let tab: string | string[] = values.tab ? values.tab : "all";
    const selectedTab = filteredTabs.filter(t => t.id === tab);
    tab = selectedTab.length > 0 ? selectedTab[0].id : "all";
    setActiveTab(tab);
    dispatch(workbenchTabCounts(filters));
  }, []);

  useEffect(() => {
    let settings: any = {
      title: "Issues",
      action_buttons: {
        new_issue: {
          type: "primary",
          label: "New Issue",
          hasClicked: false,
          buttonHandler: () => {
            window.location.replace(BASE_UI_URL.concat(`${getWorkitemDetailPage()}?new=1`));
          }
        }
      }
    };
    dispatch(setPageSettings(props.location.pathname, settings));
  }, []);

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = get(workItemBulkDeleteState, "0", { loading: true, error: true });
      if (!loading) {
        if (!error) {
          const data = get(workItemBulkDeleteState, ["0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(state => state + 1);
          } else {
            setSelectedIds([]);
            setReload(state => state + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [workItemBulkDeleteState]);

  useEffect(() => {
    if (tabCountLoading) {
      const loading = get(tabCountState, ["loading"], true);
      const error = get(tabCountState, ["error"], true);
      if (!loading && !error) {
        const data = get(tabCountState, ["data"], null);
        setTabCountLoading(false);
        setTabCounts(data);
      }
    }

    if (Object.keys(filterFromLocation).length && !formName.length) {
      const ids = mapFiltersToIds(filterFromLocation);
      const formName = `workItem_filters_map_${uuid()}`;
      if (Object.keys(ids).length) {
        dispatch(getIdsMap(formName, ids));
        setFormName(formName);
        setFiltersLoading(true);
      }
    }

    if (!tabCountLoading && filtersLoading && formName.length) {
      const ids = mapFiltersToIds(filterFromLocation);
      const formState = curFormState || {};
      if (Object.keys(ids).length === Object.keys(formState).length) {
        let moreFilters = {};
        Object.keys(ids).forEach(key => {
          const data = formState[key] || [];
          let selectedUserFilters = {};
          if (key === "user_ids") {
            selectedUserFilters = mapUserIdsToAssigneeOrReporter(data, filterFromLocation);
            moreFilters = {
              ...moreFilters,
              ...selectedUserFilters
            };
          } else {
            moreFilters = {
              ...moreFilters,
              [key]: data.map((res: any) => ({ key: res.id, label: res.name }))
            };
          }
        });
        setFiltersLoading(false);
        setMappedApiFilters(moreFilters);
      }
    }
  });

  useEffect(() => {
    if (savingFilters) {
      const loading = get(filterSaveState, "loading", true);
      const error = get(filterSaveState, "error", true);
      if (!loading && !error) {
        setSavingFilters(false);
        setUpdateInitialFilters(false);
        notification.success({ message: "Filters saved successfully!" });
      }
    }
  }, [filterSaveState]);

  useEffect(() => {
    if (defaultFiltersLoading) {
      const loading = get(filterDefaultListState, "loading", true);
      const error = get(filterDefaultListState, "error", true);
      if (!loading && !error) {
        const records = get(filterDefaultListState, ["data", "records"], []);
        if (records.length === 0) {
          if (!noDefaultFilterFound) {
            dispatch(listIssueFilter({}, "0"));
            setNoDefaultFilterFound(true);
            return;
          } else {
            setDefaultFiltersLoading(false);
            history.push({
              search: "?" + buildQueryParamsFromObject({ tab: activeTab })
            });
            return;
          }
        }

        let data = get(filterDefaultListState, ["data", "records", 0, "filter"], {});
        data = sanitizeTimeFilter(data);
        const filterName = get(filterDefaultListState, ["data", "records", 0, "name"], undefined);
        const description = get(filterDefaultListState, ["data", "records", 0, "description"], "");

        setFilterDescription(description);
        setDefaultFiltersLoading(false);
        data.page = 1;

        history.push({
          search: "?" + buildQueryParamsFromObject({ ...data, tab: activeTab })
        });

        setSelectedFilter(filterName);
        setIsDefaultFilter(!noDefaultFilterFound);
      }
    }
  }, [filterDefaultListState]);

  useEffect(() => {
    if (loadingSavedFilters) {
      const loading = get(selectedFilterState, "loading", true);
      const error = get(selectedFilterState, "error", true);

      if (!loading && !error) {
        let data = get(selectedFilterState, ["data", "filter"], {});
        const description = get(selectedFilterState, ["data", "description"], "");

        data = sanitizeTimeFilter(data);

        setLoadingSavedFilters(false);
        data.page = 1;

        setFilterDescription(description);

        history.push({
          search: "?" + buildQueryParamsFromObject({ ...data, tab: activeTab, filter_name: selectedFilter })
        });

        setIsDefaultFilter(get(selectedFilterState, ["data", "is_default"], false));
      }
    }
  }, [selectedFilterState]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("workitem", "list", "0"));
      dispatch(restapiClear("workitem", "delete", "-1"));
      dispatch(restapiClear("workitem", "bulkDelete", "-1"));
      if (formName.length > 0) {
        dispatch(formClear(formName));
      }
    };
  }, [formName]);

  const clearDefaultData = () => {
    dispatch(restapiClear("issue_filters", "list", DEFAULT_FILTER_UUID));
  };

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const onBulkDelete = () => {
    dispatch(workItemBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  const onSelectChange = (rowKeys: string[]) => {
    setSelectedIds(rowKeys);
  };

  const handleFilterSave = () => {
    const filter: { [key: string]: any } | undefined = parseQueryParamsIntoKeys(location.search, queryParamsToParse);
    filter && delete filter.tab;
    filter && delete filter.page;
    if (selectedFilter) {
      dispatch(
        updateIssueFilter(selectedFilter, {
          filter,
          description: filterDescription,
          is_default: isDefaultFilter
        })
      );
      setSavingFilters(true);
      setUpdateInitialFilters(true);
    } else {
      setCreateFilterWithData(filter);
    }
  };

  const handleItemClick = useCallback(
    (name: string, item: any) => {
      setUpdateInitialFilters(true);
      const params = buildQueryParamsFromObject({
        tab: activeTab,
        filter_name: name,
        ...(item?.data?.filter || {})
      });
      history.push({
        search: "?" + params
      });
      setSelectedFilter(name);
      setIsDefaultFilter(!!item?.data?.is_default);
      setFilterDescription(item?.data?.description);
    },
    [activeTab]
  );

  const handleCreateItem = useCallback(
    (name: string, item: any, action: string) => {
      if (action === "create") {
        const params = buildQueryParamsFromObject({
          tab: activeTab,
          filter_name: name,
          ...(item.filter || {})
        });
        history.push({
          search: "?" + params
        });
        setSelectedFilter(name);
        setIsDefaultFilter(!!item.is_default);
        setFilterDescription(item?.description);
        if (Object.keys(createFilterWithData || {}).length > 0) {
          setCreateFilterWithData({});
          setSavingFilters(false);
          setUpdateInitialFilters(false);
        }
        setUpdateInitialFilters(true);
      }
    },
    [createFilterWithData, activeTab]
  );

  const handleCloneItem = useCallback(
    (item: any) => {
      const filter_name = item.name;
      const filters = item.data.filter;

      const params = buildQueryParamsFromObject({ tab: activeTab, filter_name, ...filters });
      history.push({
        search: "?" + params
      });
      setSelectedFilter(filter_name);
      setIsDefaultFilter(!!item?.is_default);
      setFilterDescription(item?.description);
    },
    [activeTab]
  );

  const handleDefaultChange = useCallback(
    (name: string) => {
      setIsDefaultFilter(name === selectedFilter);
      clearDefaultData();
    },
    [selectedFilter]
  );

  const handleEditItem = useCallback((data: any) => {
    setFilterDescription(data.description);
    setIsDefaultFilter(data.is_default);
  }, []);

  const handleItemDelete = useCallback(
    (name: string, filterList: TriageFilterDropDownList[]) => {
      clearDefaultData();
      let _id = "";
      let _description = "";
      let _default = false;

      if (name === selectedFilter) {
        const filter = filterList.find(filter => filter.id !== selectedFilter);
        if (filter) {
          const { id, data } = filter;
          const params = buildQueryParamsFromObject({
            tab: activeTab,
            filter_name: id,
            ...data.filter
          });
          history.push({
            search: "?" + params
          });
          _id = id;
          _description = filter.description;
          _default = filter.default;
        } else {
          const params = buildQueryParamsFromObject({
            tab: activeTab
          });
          history.push({
            search: "?" + params
          });
        }

        setSelectedFilter(_id);
        setFilterDescription(_description);
        setIsDefaultFilter(_default);
      }
    },
    [selectedFilter]
  );

  const doCreateAction = useCallback(() => {
    setSavingFilters(true);
    setUpdateInitialFilters(true);
  }, []);

  const onTabChange = useCallback(
    (key: string) => {
      const tab = filteredTabs.find(tab => tab.id === key);
      let activeTab = "all";
      let moreFilters: any = {};
      if (tab) {
        activeTab = key;
        moreFilters = tab.filters;
        if (moreFilters.assignee_user_ids) {
          moreFilters.assignee_user_ids = [
            {
              label: ls.getUserEmail(),
              key: ls.getUserId()
            }
          ];
        }
      }
      // updating filters for Since Last Week
      if (activeTab === "new") {
        const { $gt, $lt } = moreFilters?.updated_at;
        moreFilters = {
          ...moreFilters,
          ...(moreFilters.filters || {}),
          updated_at: {
            $gt: moment.unix($gt).format(DATE_RANGE_FILTER_FORMAT),
            $lt: moment.unix($lt).format(DATE_RANGE_FILTER_FORMAT)
          }
        };
      }

      const appliedFilter = getFilterDataFromId(selectedFilter as string);

      if (selectedFilter && Object.keys(appliedFilter).length > 0) {
        moreFilters = {
          ...moreFilters,
          ...appliedFilter,
          filter_name: selectedFilter
        };
      }

      const params = buildQueryParamsFromObject(queryParamsFromFilters(moreFilters, activeTab || "all"));

      props.history.push({
        search: "?" + params
      });
      setActiveTab(activeTab);
      setSelectedIds([]);
    },
    [filteredTabs, selectedFilter, filterListState]
  );

  const getFilters = (key: string) => {
    const ls = new LocalStoreService();
    const tab = filteredTabs.find(tab => tab.id === key);
    let moreFilters: any = {};
    if (tab) {
      moreFilters = { ...tab.filters };
      if (moreFilters.assignee_user_ids) {
        moreFilters.assignee_user_ids = [
          {
            label: ls.getUserEmail(),
            key: ls.getUserId()
          }
        ];
      }
    }
    return moreFilters;
  };

  const mappedTabs = filteredTabs.map((tab, i) => ({
    ...tab,
    tab: !tabCountLoading && tabCounts ? `${tab.label} (${tabCounts[i]})` : tab.label
  }));

  const nonApiFilters = mapNonApiFilters(filterFromLocation);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange
    }),
    [selectedIds]
  );

  const titleComponent = useMemo(
    () => (
      <IssueFilterDropDown
        uuid={`${ISSUES_UUID}@${activeTab}`}
        filterName={selectedFilter as string}
        filterDescription={filterDescription}
        isDefault={isDefaultFilter}
        handleItemClick={handleItemClick}
        handleCreateItem={handleCreateItem}
        handleCloneItem={handleCloneItem}
        handleDefaultChange={handleDefaultChange}
        handleEditItem={handleEditItem}
        handleItemDelete={handleItemDelete}
        createFilterWithData={createFilterWithData}
        doCreateAction={doCreateAction}
      />
    ),
    [activeTab, selectedFilter, isDefaultFilter, filterDescription, createFilterWithData, handleDefaultChange]
  );

  if (filtersLoading || tabCountLoading) {
    return <Loader />;
  }

  return (
    <div className={"content"}>
      <Tabs onChange={onTabChange} activeKey={activeTab} size="small" animated={false}>
        {filteredTabs.map((tab, i) => (
          <TabPane key={tab.id} tab={mappedTabs[i].tab}>
            <WorkitemList
              moreFilters={{ ...getFilters(tab.id), ...nonApiFilters, ...mappedApiFilters }}
              uuid={`${ISSUES_UUID}@${tab.id}`}
              tab={mappedTabs[i].tab}
              rowSelection={rowSelection}
              clearSelectedIds={clearSelectedIds}
              onBulkDelete={onBulkDelete}
              reload={reload}
              selectedIds={selectedIds}
              bulkDeleting={bulkDeleting}
              componentTitle={titleComponent}
              savingFilters={savingFilters}
              handleFilterSave={handleFilterSave}
              updateInitialFilters={updateInitialFilters}
              setUpdateInitialFilters={setUpdateInitialFilters}
              disableSaveFilterButton={false}
            />
          </TabPane>
        ))}
      </Tabs>
    </div>
  );
};

export default WorkItemListPage;
