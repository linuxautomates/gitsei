import React from "react";
import { connect } from "react-redux";
import { Link } from 'react-router-dom';
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getError, getLoading } from "utils/loadingUtils";
import Loader from "components/Loader/Loader";
import { AntCol, AntRow, AntText, AntTitle } from "shared-resources/components";
import { Card } from "antd";
import LocalStoreService from "services/localStoreService";
import { WorkitemList } from "workitems/containers";
import { getWorkitemsPage, getReportsPage } from 'constants/routePaths';

export class DashboardOverviewContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      open_items_loading: true,
      unassigned_items_loading: true,
      last_week_items_loading: true,
      questionnaires_loading: true
    };
  }

  componentDidMount() {
    const lastWeek = Math.ceil(new Date().getTime() / 1000 - 604800);
    const openWorkItems = { page: 0, page_size: 1, filter: { status: "NEW" } };
    const unassignedWorkItems = { page: 0, page_size: 1, filter: { unassigned: true, status: "NEW" } };
    const lastWeekWorkItems = { page: 0, page_size: 1, filter: { created_after: lastWeek } };
    const allQuestionnaires = { page: 0, page_size: 1 };
    this.setState(
      {
        open_items_loading: true,
        unassigned_items_loading: true,
        last_week_items_loading: true,
        questionnaires_loading: true
      },
      () => {
        this.props.workItemList(openWorkItems, "openitems");
        this.props.workItemList(unassignedWorkItems, "unassigneditems");
        this.props.workItemList(lastWeekWorkItems, "lastweekitems");
        this.props.quizList(allQuestionnaires);
      }
    );
  }

  componentWillUnmount() {
    this.props.restapiClear("workitem", "list", "-1");
    this.props.restapiClear("quiz", "list", "0");
  }

  static getDerivedStateFromProps(props, state) {
    let openItemsLoading = state.open_items_loading;
    let unassignedItemsLoading = state.unassigned_items_loading;
    let lastWeekItemsLoading = state.last_week_items_loading;
    let questionnairesLoading = state.questionnaires_loading;
    if (state.open_items_loading) {
      const loading = getLoading(props.rest_api, "workitem", "list", "openitems");
      const error = getError(props.rest_api, "workitem", "list", "openitems");
      if (!loading && !error) {
        openItemsLoading = false;
      }
    }
    if (state.unassigned_items_loading) {
      const loading = getLoading(props.rest_api, "workitem", "list", "unassigneditems");
      const error = getError(props.rest_api, "workitem", "list", "unassigneditems");
      if (!loading && !error) {
        unassignedItemsLoading = false;
      }
    }

    if (state.last_week_items_loading) {
      const loading = getLoading(props.rest_api, "workitem", "list", "lastweekitems");
      const error = getError(props.rest_api, "workitem", "list", "lastweekitems");
      if (!loading && !error) {
        lastWeekItemsLoading = false;
      }
    }

    if (state.questionnaires_loading) {
      const loading = getLoading(props.rest_api, "quiz", "list", "0");
      const error = getError(props.rest_api, "quiz", "list", "0");
      if (!loading && !error) {
        questionnairesLoading = false;
      }
    }

    return {
      ...state,
      open_items_loading: openItemsLoading,
      unassigned_items_loading: unassignedItemsLoading,
      questionnaires_loading: questionnairesLoading,
      last_week_items_loading: lastWeekItemsLoading
    };
  }

  render() {
    if (
      this.state.open_items_loading ||
      this.state.unassigned_items_loading ||
      this.state.questionnaires_loading ||
      this.state.last_week_items_loading
    ) {
      return <Loader />;
    }
    const openItems = this.props.rest_api.workitem.list.openitems.data._metadata?.total_count || 0;
    const unassignedItems = this.props.rest_api.workitem.list.unassigneditems.data._metadata?.total_count || 0;
    const lastWeekItems = this.props.rest_api.workitem.list.lastweekitems.data._metadata?.total_count || 0;
    const questionnaires = this.props.rest_api.quiz.list?.["0"].data._metadata?.total_count || 0;
    let ls = new LocalStoreService();
    const userId = ls.getUserId();
    const userEmail = ls.getUserEmail();
    const moreFilters = {
      assignee_user_ids: [
        {
          key: userId,
          label: userEmail
        }
      ],
      status: "NEW"
    };

    return (
      <div>
        <AntRow className="mb-20">
          <AntCol span={24}>
            <AntTitle level={4}>Work Overview</AntTitle>
            <Card>
              <div>
                <AntRow type={"flex"} justify={"space-between"}>
                  <AntCol span={6} align={"center"}>
                    <AntRow>
                      <AntText type={"secondary"} strong>
                        OPEN WORK ITEMS
                      </AntText>
                    </AntRow>
                    <AntRow>
                      <AntText style={{ fontSize: "40px" }}>
                        <Link to={`${getWorkitemsPage()}?tab=open`}>{openItems}</Link>
                      </AntText>
                    </AntRow>
                  </AntCol>
                  <AntCol span={6} align={"center"}>
                    <AntRow>
                      <AntText type={"secondary"} strong>
                        NEW SINCE LAST WEEK
                      </AntText>
                    </AntRow>
                    <AntRow>
                      <AntText style={{ fontSize: "40px" }}>
                        <Link to={`${getWorkitemsPage()}?tab=new`}>{lastWeekItems}</Link>
                      </AntText>
                    </AntRow>
                  </AntCol>
                  <AntCol span={6} align={"center"}>
                    <AntRow>
                      <AntText type={"secondary"} strong>
                        UNASSIGNED ITEMS
                      </AntText>
                    </AntRow>
                    <AntRow>
                      <AntText style={{ fontSize: "40px" }}>
                        <Link to={`${getWorkitemsPage()}?tab=unassigned`}>{unassignedItems}</Link>
                      </AntText>
                    </AntRow>
                  </AntCol>
                  <AntCol span={6} align={"center"}>
                    <AntRow>
                      <AntText type={"secondary"} strong>
                        ASSESSMENTS
                      </AntText>
                    </AntRow>
                    <AntRow>
                      <AntText style={{ fontSize: "40px" }}>
                        <Link to={getReportsPage()}>{questionnaires}</Link>
                      </AntText>
                    </AntRow>
                  </AntCol>
                </AntRow>
              </div>
            </Card>
          </AntCol>
        </AntRow>
        <AntRow className={"mb-20"}>
          <AntCol span={24}>
            <WorkitemList
              pageName={"dashboard_workitems"}
              moreFilters={moreFilters}
              partialFilters={{}}
              hasSearch={false}
              hasFilters={false}
              title={"Assigned to Me"}
            />
          </AntCol>
        </AntRow>
      </div>
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(DashboardOverviewContainer);
