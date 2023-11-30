import React, { Component } from "react";
import { connect } from "react-redux";
import { List, Popconfirm } from "antd";

import { AntCol, AntRow, AntText, InfiniteScroll } from "shared-resources/components";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { QuizDetail } from "assessments/components";
import { tableCell } from "utils/tableUtils";
import { getQuizAnswerPage } from "constants/routePaths";
import { workitemFlowDispatchToProps } from "reduxConfigs/maps/workitemFlowMap";
import "./assessment-infinite-scroll.component.scss";
import { AntButtonComponent } from "../../../shared-resources/components/ant-button/ant-button.component";
import { quizDeleteSelector } from "reduxConfigs/selectors/quizSelector";
import { restAPILoadingState } from "../../../utils/stateUtil";
import { getQnAProgress } from "../../utils/helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";

export class AssessmentInfiniteScrollComponent extends Component {
  constructor(props) {
    super(props);

    this.state = {
      delete_loading: false,
      q_id: undefined,
      expand: [],
      reload: 1
    };

    this.renderQuiz = this.renderQuiz.bind(this);
    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.onDetails = this.onDetails.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.delete_loading) {
      const { loading, error } = restAPILoadingState(props.quizDeleteState, state.q_id.toString());
      if (!loading && !error) {
        return {
          reload: state.reload + 1,
          delete_loading: false,
          q_id: undefined
        };
      }
    }

    return null;
  }

  buildActions(id) {
    return (
      <AntRow type={"flex"} justify={"space-between"}>
        <AntButtonComponent
          type="link"
          className={"mx-5"}
          style={{ padding: 0, marginLeft: "10px", fontSize: "13px" }}
          onClick={() => this.onEditHandler(id)}>
          View/Edit Assessment
        </AntButtonComponent>
        {getRBACPermission(PermeableMetrics.WORKITEM_ASSESSMENT_DELETE) && (
          <Popconfirm
            title={"Do you want to delete this item?"}
            onConfirm={() => this.onRemoveHandler(id)}
            okText={"Yes"}
            cancelText={"No"}>
            <AntButtonComponent type="link" className={"mx-5"} style={{ padding: 0, fontSize: "13px" }}>
              Delete Assessment
            </AntButtonComponent>
          </Popconfirm>
        )}
      </AntRow>
    );
  }

  onEditHandler(qId) {
    let url = getQuizAnswerPage().concat(`?questionnaire=${qId}`);
    window.open(url);
  }

  onRemoveHandler(qId) {
    this.setState(
      {
        delete_loading: true,
        q_id: qId
      },
      () => {
        this.props.quizDelete(qId);
      }
    );
  }

  onDetails(qId) {
    return e => {
      e.preventDefault();
      let expand = this.state.expand;
      if (!expand.includes(qId)) {
        expand.push(qId);
        this.setState({ expand: expand });
      }
    };
  }

  renderQuiz(quiz) {
    // const { loading, error } = loadingStatus(this.props.rest_api, "quiz", "list", this.props.uuid);
    // if (!loading && !error) {
    return (
      <AntRow type={"flex"} key={quiz.id} style={{ width: "100%" }}>
        <AntCol span={24}>
          <AntRow type={"flex"} justify={"space-between"}>
            <AntCol span={24}>
              <AntText style={{ color: "#8a94a5", fontSize: "14px" }}>{quiz.vanity_id}</AntText>
            </AntCol>
          </AntRow>
          <AntRow type={"flex"}>
            <AntCol span={18}>
              <div style={{ display: "flex" }}>
                <AntText strong style={{ display: "flex", flexDirection: "column", justifyContent: "center" }}>
                  {/*eslint-disable-next-line  jsx-a11y/anchor-is-valid*/}
                  <a href={"#"} onClick={this.onDetails(quiz.id)}>
                    {quiz.questionnaire_template_name}
                  </a>
                </AntText>
                {this.buildActions(quiz.id)}
              </div>
            </AntCol>
            <AntCol span={6}>{tableCell("quiz_progress", getQnAProgress(quiz))}</AntCol>
          </AntRow>
          {this.state.expand.includes(quiz.id) && (
            <AntRow type={"flex"}>
              <AntCol span={24}>
                <QuizDetail quiz_id={quiz.id} delete_loading={this.state.delete_loading} delete_id={this.state.q_id} />
              </AntCol>
            </AntRow>
          )}
        </AntCol>
      </AntRow>
    );
    // } else return null;
  }

  render() {
    return (
      <InfiniteScroll
        uri="quiz"
        pageSize={50}
        horizontal={false}
        className={"assessment-infinite-scroll"}
        derive={true}
        shouldDerive={["work_item_id"]}
        reload={this.state.reload}
        filters={this.props.moreFilters}
        renderItem={item => <List.Item>{this.renderQuiz(item)}</List.Item>}
        dataFilter={this.props.dataFilter}
        uuid={this.props.uuid}
        loadOnMount={false}
      />
    );
  }
}

const mapDispatchToProps = dispatch => ({
  ...workitemFlowDispatchToProps(dispatch),
  ...mapRestapiDispatchtoProps(dispatch)
});

const mapStateToProps = state => ({
  ...mapSessionStatetoProps(state),
  quizDeleteState: quizDeleteSelector(state)
});

export default connect(mapStateToProps, mapDispatchToProps)(AssessmentInfiniteScrollComponent);
