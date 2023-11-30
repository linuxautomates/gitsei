import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import { quizListState, subTicketsListState } from "reduxConfigs/selectors/restapiSelector";
import { Row, Col, Tabs, notification, Button } from "antd";
import { debounce, get } from "lodash";
import { AssessmentInfiniteScroll } from "assessments/components";
import { CreateIssue, SubTasks } from "../index";
import { AntCheckbox } from "../../../../../shared-resources/components";
import { AuditLogs, Notes } from "workitems/containers";
import { RouteComponentProps, withRouter } from "react-router-dom";
import { getWorkitemDetailPage } from "constants/routePaths";

const { TabPane } = Tabs;

interface SmartTicketTabsProps extends RouteComponentProps {
  workItemId: string;
  workItemParentId: string;
  workItemChildren: any[];
  productId: string;
  className?: string;
  onReroute: (url: string) => void;
  onRefresh: () => void;
}

export const SmartTicketsTabsContainer: React.FC<SmartTicketTabsProps> = ({
  workItemId,
  workItemChildren = [],
  className = "work-items-page",
  workItemParentId = undefined,
  ...props
}) => {
  const [show_more_assessments, setShowMoreAssessments] = useState(false);
  const [assessment_notified, setAssessmentNotified] = useState(false);
  const [_workItemId, setWorkItemId] = useState<undefined | string>(undefined);
  const [create_modal, setCreateModal] = useState(false);

  const assessments = useSelector(state => quizListState(state, { workItemId }));
  const subTickets = useSelector(state => subTicketsListState(state, { workItemId }));

  const debouncedNotification = debounce(() => {
    notification.info({ message: "No Additional Assessments Found" });
  }, 1000);

  useEffect(() => {
    if (workItemId !== _workItemId) {
      setWorkItemId(workItemId);
      setShowMoreAssessments(false);
      setAssessmentNotified(false);
      setCreateModal(false);
    }
  }, [props]); // eslint-disable-line react-hooks/exhaustive-deps

  const createIssueModal = () => {
    if (!create_modal) {
      return null;
    }
    return (
      <CreateIssue
        product={props.productId}
        parent_id={workItemId}
        location={props.location}
        onCancelEvent={toggleCreateIssueModal}
        onSuccessEvent={(newWorkId: any) => {
          toggleCreateIssueModal();
          props.onRefresh();
          props.onReroute(`${getWorkitemDetailPage()}?workitem=${newWorkId}`);
        }}
        isVisible
      />
    );
  };

  const toggleCreateIssueModal = () => {
    setCreateModal(state => !state);
  };

  const assessmentsTab = () => {
    let workItemIds = [workItemId, ...workItemChildren];
    if (workItemParentId) {
      workItemIds.push(workItemParentId);
    }
    let filteredIds = [];

    if (!show_more_assessments) {
      filteredIds.push(...workItemChildren);
      if (workItemParentId) {
        filteredIds.push(workItemParentId);
      }
    }
    const moreFilters = {
      work_item_ids: workItemIds
    };

    const showCheckbox = workItemParentId !== undefined || workItemChildren.length > 0;
    return (
      <Row type={"flex"}>
        {showCheckbox && (
          <Col span={24}>
            <div>
              <AntCheckbox
                checked={show_more_assessments}
                onChange={(e: any) => {
                  setShowMoreAssessments(e.target.checked);
                  let currentCount = 0;
                  const quizList = assessments;
                  const loading = quizList.loading;
                  if (!loading) {
                    if (quizList.data._metadata.total_count) {
                      // count is actually the record of if anything is available in the filtered data
                      currentCount = quizList.data._metadata.total_count;
                      let filteredIds: any[] = [];
                      filteredIds.push(...workItemChildren);
                      if (workItemParentId) {
                        filteredIds.push(workItemParentId);
                      }
                      if (filteredIds.length > 0) {
                        currentCount = quizList.data.records.filter(
                          (record: any) => !filteredIds.includes(record.work_item_id)
                        ).length;
                      }
                      if (!show_more_assessments) {
                        // this.setState({ all_count: currentCount });
                      } else {
                        const allCount = quizList.data._metadata.total_count;
                        if (allCount === currentCount && !assessment_notified) {
                          setAssessmentNotified(true);
                          debouncedNotification();
                        }
                      }
                    }
                  }
                }}>
                {workItemParentId === undefined && "Show Sub-Issue(s) Assessments"}
                {workItemParentId !== undefined && "Show Parent Issue Assessments"}
              </AntCheckbox>
            </div>
          </Col>
        )}

        <Col span={24}>
          {" "}
          <AssessmentInfiniteScroll
            moreFilters={moreFilters}
            uuid={workItemId}
            parentId={workItemParentId}
            dataFilter={{ field: "work_item_id", values: filteredIds }}
          />
        </Col>
      </Row>
    );
  };

  const subTicketsTab = () => {
    return (
      <Row type={"flex"}>
        <Col span={24}>
          <Row type={"flex"} justify={"end"}>
            <Col span={6}>
              <Button type={"link"} icon={"plus-circle"} onClick={e => setCreateModal(true)}>
                Create Sub-Issue
              </Button>
            </Col>
          </Row>
        </Col>
        <Col span={24}>
          <SubTasks workItemId={workItemId} workItemChildren={workItemChildren} onRefresh={props.onRefresh} />
        </Col>
      </Row>
    );
  };

  const activityTab = () => {
    return (
      <Row type={"flex"}>
        <Col span={24}>
          <AuditLogs workItemId={workItemId} />
        </Col>
      </Row>
    );
  };

  const notesTab = () => {
    return (
      <Row type={"flex"}>
        <Col span={24}>
          <Notes workItemid={workItemId} />
        </Col>
      </Row>
    );
  };

  if (!workItemId) {
    return null;
  }
  const assessmentCount = get(assessments, ["data", "_metadata", "total_count"], 0);
  const subIssuesCount = get(subTickets, ["data", "_metadata", "total_count"], 0);

  const tabsMap = [
    {
      id: "assessments",
      label: `Assessments (${assessmentCount})`,
      tab: assessmentsTab()
    },
    {
      id: "sub-tickets",
      label: `Sub-Tickets (${subIssuesCount})`,
      tab: subTicketsTab()
    },
    {
      id: "comments",
      label: `Comments`,
      tab: notesTab()
    },
    {
      id: "activity",
      label: `Activity`,
      tab: activityTab()
    }
  ];
  return (
    <>
      {create_modal && createIssueModal()}
      <Tabs size="small" onChange={() => {}} style={{ overflow: "auto" }} animated={false}>
        {tabsMap.map(tab => {
          return (
            <TabPane key={tab.id} tab={tab.label} forceRender={false}>
              {tab.tab}
            </TabPane>
          );
        })}
      </Tabs>
    </>
  );
};

export default withRouter(SmartTicketsTabsContainer);
