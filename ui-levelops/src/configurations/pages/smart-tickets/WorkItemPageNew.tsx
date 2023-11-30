import { List, Result } from "antd";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { restapiClear, restapiData } from "reduxConfigs/actions/restapi";
import { getWorkItemsListSelector, workItemListState } from "reduxConfigs/selectors/restapiSelector";
import LocalStoreService from "services/localStoreService";
import { AntButton, AntCol, AntInput, AntRow, AntTabs, InfiniteScroll } from "shared-resources/components";
import { WorkItemListCard } from "./components";
import { CreateIssue, SmartTicketChange } from "./containers";
import queryString from "query-string";
import { get, isEqual } from "lodash";
import "./workitems.style.scss";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { BASE_UI_URL } from "helper/envPath.helper";
import { getWorkitemDetailPage } from "constants/routePaths";

const WorkItemPageNew: React.FC<RouteComponentProps> = props => {
  const [page, setPage] = useState<number>(0);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [createModel, setCreateModel] = useState<any>(false);
  const [selectedWorkItem, setSelectedWorkItem] = useState<any>(null);
  const [activeKey, setActiveKey] = useState<string>("all");
  const [reload, setReload] = useState<number>(1);

  const dispatch = useDispatch();
  const workItemsState = useSelector(getWorkItemsListSelector);
  const [workItems, setWorkItems] = useState(workItemsState);

  const workItemListGetState = useSelector((state: any) => workItemListState(state, "0"));

  const ls = new LocalStoreService();

  useEffect(() => {
    const settings: any = {
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

    return () => {
      dispatch(restapiClear("workitem", "list", "-1"));
      dispatch(restapiClear("workitem", "get", "-1"));
      dispatch(restapiClear("tags", "list", "-1"));
      dispatch(restapiClear("products", "list", "-1"));
      dispatch(restapiClear("ticket_templates", "list", "-1"));
      dispatch(restapiClear("users", "get", "-1"));
      dispatch(restapiClear("states", "list", "workitems"));
      dispatch(restapiClear("products", "list", "workitems"));
    };
  }, []);

  useEffect(() => {
    const workItem = workItems.find((item: any) => item.vanity_id === selectedWorkItem);
    if (workItem) {
      if (
        (activeKey === "reported_by_me" && workItem.reporter !== ls.getUserEmail()) ||
        (activeKey === "assigned_to_me" &&
          !workItem.assignees.map((assignee: any) => assignee.user_id).includes(ls.getUserId()))
      ) {
        setSelectedWorkItem(null);
        props.history.push(getWorkitemDetailPage());
      }
    } else if (activeKey !== "all") {
      props.history.push(getWorkitemDetailPage());
    }
  }, [activeKey]);

  useEffect(() => {
    const workitemIdInProps = queryString.parse(props.location.search).workitem;
    const shouldCreateNew = queryString.parse(props.location.search).new;
    const isDeleted = queryString.parse(props.location.search).delete;
    const allowCreateNew = shouldCreateNew !== undefined;
    if (isDeleted) {
      const deletedItem = workItemsState.find((item: any) => item.vanity_id === isDeleted);
      let newSelectedWorkItem = null;
      if (workItemsState.length > 0) {
        const filteredItem = workItemsState.filter(
          (item: any) => item.vanity_id !== isDeleted && item.parent_id !== deletedItem?.id
        );
        if (filteredItem.length > 0) {
          newSelectedWorkItem = filteredItem[0].vanity_id;
        }
      }
      props.history.push(getWorkitemDetailPage());
      setReload(prev => prev + 1);
      setSelectedWorkItem(newSelectedWorkItem);
    }
    if (allowCreateNew) {
      setCreateModel(true);
    }
    if (workitemIdInProps && workitemIdInProps !== selectedWorkItem) {
      setSelectedWorkItem(workitemIdInProps);
      setCreateModel(allowCreateNew);
    }
    if (workItemsState.length) {
      if (!workItems.length || !isEqual(workItemsState, workItems)) {
        setWorkItems(workItemsState);
        setCreateModel(allowCreateNew);
      }
    } else {
      setSelectedWorkItem(null);
    }

    if (!workitemIdInProps && !selectedWorkItem && !createModel) {
      const loading = get(workItemListGetState, ["loading"], true);
      if (!loading && workItemsState.length > 0) {
        let workitemId = workItemsState[0].vanity_id;
        setSelectedWorkItem(workitemId);
      }
    }
  }, [props.location.search, workItemsState]);

  const onSelectWorkItem = (selected_workItem: any) => {
    const workItemObj = (workItemsState || []).find((item: any) => item.vanity_id === selected_workItem);
    if (workItemObj) {
      // @ts-ignore
      dispatch(restapiData(workItemObj, "workitem", "get", `list/vanity-id/${selected_workItem}`));
    }
    setSelectedWorkItem(selected_workItem);
    props.history.push(`${getWorkitemDetailPage()}?workitem=${selected_workItem}`);
  };

  const onToggleCreateModal = () => {
    setCreateModel((prevState: any) => !prevState);
  };

  const onSearchHandler = (text: string) => {
    setSearchTerm(text);
  };

  const onLoadNext = () => {
    setPage(prevPage => prevPage + 1);
  };

  const createIssueModal = () => {
    if (!createModel) {
      return null;
    }
    return (
      <CreateIssue
        location={props.location}
        onCancelEvent={(e: any) => {
          onToggleCreateModal();
          if (selectedWorkItem) {
            props.history.push(`${getWorkitemDetailPage()}?workitem=${selectedWorkItem}`);
          } else {
            props.history.push(getWorkitemDetailPage());
          }
        }}
        onSuccessEvent={(newWorkId: any) => {
          onToggleCreateModal();
          setReload(prev => prev + 1);
          props.history.push(`${getWorkitemDetailPage()}?workitem=${newWorkId}`);
        }}
        isVisible
        allowCreateAssignee={true}
      />
    );
  };

  const editPage = () => {
    return (
      <AntCol span={18} className="h-100">
        <div className="h-100">
          {selectedWorkItem && (
            <>
              <SmartTicketChange
                vanityId={selectedWorkItem}
                workId={selectedWorkItem}
                history={props.history}
                onReroute={(url: string) => props.history.push(url)}
                onRefresh={() => {
                  dispatch(restapiClear("workitem", "get", "-1"));
                  setReload((prev: number) => prev + 1);
                }}
              />
            </>
          )}
          {!selectedWorkItem && (
            <Result
              status="404"
              title="No Issues"
              subTitle="No issues selected or available"
              extra={
                <>
                  <AntButton type="primary" onClick={() => props.history.push(`${getWorkitemDetailPage()}?new=1`)}>
                    Create New Issue
                  </AntButton>
                </>
              }
            />
          )}
        </div>
      </AntCol>
    );
  };

  const tabPanes = () => {
    return [
      {
        id: "all",
        tab: "All",
        filter: {}
      },
      {
        id: "reported_by_me",
        tab: "Reported By Me",
        filter: {
          reporter: ls.getUserEmail()
        }
      },
      {
        id: "assigned_to_me",
        tab: "My Issues",
        filter: {
          assignee_user_ids: [ls.getUserId()]
        }
      }
    ];
  };

  const tabs = tabPanes();
  const currentTab: any = tabs.find(tab => tab.id === activeKey) || {};

  return (
    <>
      <AntRow gutter={[0, 20]} type={"flex"}>
        <AntCol span={24}>
          <AntTabs size={"small"} animated={false} tabpanes={tabs} onChange={setActiveKey} />
        </AntCol>
      </AntRow>
      <div className={"work-items-page"}>
        <AntRow gutter={[40, 0]} className="h-100 work-items-page_edit-container" type={"flex"}>
          {createIssueModal()}
          <AntCol span={6}>
            <AntRow gutter={[0, 20]} type={"flex"}>
              <AntCol span={24}>
                <AntInput
                  type="search"
                  placeholder="Search..."
                  value={searchTerm}
                  onChange={(e: any) => onSearchHandler(e.target.value)}
                />
              </AntCol>
            </AntRow>

            <AntRow gutter={[0, 20]}>
              <AntCol span={24}>
                <div className={`work-items-page`}>
                  <InfiniteScroll
                    uri="workitem"
                    pageSize={20}
                    horizontal={false}
                    page={page}
                    filters={{
                      partial: { title: searchTerm },
                      ...currentTab.filter
                    }}
                    derive={false}
                    reload={reload}
                    onReachBottom={onLoadNext}
                    onReachTop={() => null}
                    className="work-items-page__list"
                    renderItem={(item: any) => (
                      <List.Item
                        className={`ant-list-item-smart-ticket ${
                          item.vanity_id === selectedWorkItem ? "selected" : ""
                        }`}>
                        <WorkItemListCard key={item.id} workItem={item} onSelectEvent={onSelectWorkItem} />
                      </List.Item>
                    )}
                  />
                </div>
              </AntCol>
            </AntRow>
          </AntCol>
          {editPage()}
        </AntRow>
      </div>
    </>
  );
};

export default WorkItemPageNew;
