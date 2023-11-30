import React, { Component } from "react";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { connect } from "react-redux";
import { getData, loadingStatus } from "utils/loadingUtils";
import { RestQuiz } from "classes/RestQuiz";
import Loader from "components/Loader/Loader";
import { Collapse, List } from "antd";
import { AntCol, AntRow, AntText, NameAvatar } from "shared-resources/components";
import PreviewerComponent from "../../../shared-resources/components/previewer/previewer";
import { AttachmentItem } from "../../../shared-resources/components/attachment-item/attachment-item.component";
import { validateURL } from "utils/stringUtils";

const { Panel } = Collapse;

export class QuizDetailComponent extends Component {
  constructor(props) {
    super(props);
    const { loading } = loadingStatus(props.rest_api, "quiz", "get", props.quiz_id);
    if (loading) {
      this.props.quizGet(props.quiz_id);
    } else {
      const quiz = getData(props.rest_api, "quiz", "get", props.quiz_id);
      if (quiz === undefined || quiz.id === undefined) {
        this.props.quizGet(props.quiz_id);
      }
    }

    this.state = {
      quiz_id: props.quiz_id,
      quiz_loading: true,
      quiz: undefined,
      showPreviewer: false,
      previewList: [],
      currentPreviewIndex: -1,
      downloadingPreviewFileIds: [],
      downloadingPreviewFiles: false
    };

    this.getQnAData = this.getQnAData.bind(this);
    this.closePreviewer = this.closePreviewer.bind(this);
    this.previewFile = this.previewFile.bind(this);
    this.handleFileDownload = this.handleFileDownload.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.quiz_loading) {
      const { loading, errors } = loadingStatus(props.rest_api, "quiz", "get", state.quiz_id);
      if (!loading && !errors) {
        const data = getData(props.rest_api, "quiz", "get", state.quiz_id);
        let quiz = new RestQuiz(data);
        return {
          ...state,
          quiz_loading: false,
          quiz: quiz
        };
      }
    }

    if (state.downloadingPreviewFiles) {
      const fileIds = state.downloadingPreviewFileIds.map(f => f.file_id);
      if (fileIds.length) {
        return fileIds.reduce(
          (acc, fileId) => {
            const { loading, error } = loadingStatus(props.rest_api, "files", "get", fileId);
            if (!loading && !error) {
              const newIds = acc.downloadingPreviewFileIds.filter(file => file.file_id !== fileId);
              return {
                ...acc,
                downloadingPreviewFileIds: newIds
              };
            }

            return acc;
          },
          { ...state }
        );
      }

      return {
        ...state,
        downloadingPreviewFiles: false
      };
    }

    return null;
  }

  closePreviewer() {
    this.setState({
      showPreviewer: false,
      previewList: [],
      currentPreviewIndex: -1
    });
  }

  handleFileDownload(e, fileId, fileName) {
    e.preventDefault();
    if (this.state.downloadingPreviewFileIds.findIndex(f => f.file_id === fileId) !== -1) {
      return;
    }
    this.props.filesGet(fileId, fileName);
    this.setState(state => ({
      ...state,
      downloadingPreviewFiles: true,
      downloadingPreviewFileIds: [...state.downloadingPreviewFileIds, { file_id: fileId, file_name: fileName }]
    }));
  }

  componentWillUnmount() {
    this.props.restapiClear("quiz", "get", "-1");
    this.props.restapiClear("files", "get", "-1");
  }

  previewFile(response, item) {
    let currentIndex = -1;
    if (!response.responses.length) {
      return;
    }

    const fileIds = response.responses
      .filter(f => f.type === "file")
      .map((res, index) => {
        let fileId = res.value;
        let fileName = res.file_name;

        let fetchFileId = `quiz/${this.state.quiz.id}/assertion/${response.question_id}/${fileId}`;

        return {
          fileId: fileId,
          fileName: fileName,
          fetchFileId: fetchFileId
        };
      });

    currentIndex = fileIds.findIndex(f => f.fileId === item.value);

    if (currentIndex !== -1) {
      this.setState({
        previewList: fileIds,
        currentPreviewIndex: currentIndex,
        showPreviewer: true
      });
    }
  }

  getQnAData(section) {
    const { quiz } = this.state;

    if (section && quiz.section_responses) {
      const sectionData = {
        questions: section.questions,
        responses: quiz.section_responses.find(res => res.section_id === section.id)
      };

      return {
        ...sectionData.questions.map(question => ({
          question,
          response: sectionData.responses.answers.find(res => res.question_id === question.id)
        }))
      };
    } else if (section) {
      return {
        ...section.questions.map(question => ({
          question
        }))
      };
    } else return {};
  }

  renderQuestions() {
    return (
      <Collapse key={this.state.quiz_id} bordered={false} accordion={true}>
        {this.state.quiz.sections.map(section => {
          const QnaData = this.getQnAData(section);
          return (
            <Panel
              key={section.id}
              header={
                <AntRow type={"flex"} justify={"start"} align={"middle"}>
                  <AntText strong>{section.name}</AntText>
                </AntRow>
              }>
              <AntText
                style={{
                  fontSize: "14px",
                  color: "#8a94a5",
                  paddingBottom: "10px"
                }}>
                {section.description}
              </AntText>
              <List itemLayout={"vertical"}>
                {Object.keys(QnaData).map(qna => {
                  return (
                    <List.Item key={QnaData[qna].question.id}>
                      <AntRow type={"flex"} style={{ margin: "10px 0" }}>
                        <AntCol span={24}>
                          <AntText style={{ fontSize: "14px" }}>{QnaData[qna].question.name}</AntText>
                        </AntCol>
                      </AntRow>
                      {QnaData[qna].response && (
                        <AntRow type={"flex"} justify={"space-between"}>
                          <AntCol span={14}>
                            {QnaData[qna].response.responses &&
                              QnaData[qna].response.responses.map((res, index) => {
                                return (
                                  <div
                                    key={index}
                                    style={{
                                      fontSize: "14px",
                                      whiteSpace: "no-wrap",
                                      overflow: "hidden",
                                      textOverflow: "ellipsis"
                                    }}>
                                    {res.file_name ? (
                                      <AttachmentItem
                                        trigger="click"
                                        fileName={res.file_name}
                                        showBorder={false}
                                        previewFile={() => this.previewFile(QnaData[qna].response, res)}
                                      />
                                    ) : res.type === "link" && validateURL(res?.value) ? (
                                      <a
                                        href={res.value}
                                        style={{
                                          fontSize: "14px",
                                          whiteSpace: "no-wrap",
                                          overflow: "hidden",
                                          textOverflow: "ellipsis"
                                        }}>
                                        {res.value}
                                      </a>
                                    ) : (
                                      <AntText style={{ fontWeight: "bold", fontSize: "14px" }}>{res.value}</AntText>
                                    )}
                                  </div>
                                );
                              })}
                          </AntCol>
                          <AntCol span={10}>
                            <AntRow type={"flex"} justify={"end"} align={"middle"}>
                              {QnaData[qna].response.user_email && (
                                <>
                                  <NameAvatar name={QnaData[qna].response.user_email} />
                                  <AntText ellipsis style={{ fontSize: "12px", marginLeft: "10px" }}>
                                    {QnaData[qna].response.user_email}
                                  </AntText>
                                </>
                              )}
                            </AntRow>
                          </AntCol>
                        </AntRow>
                      )}
                    </List.Item>
                  );
                })}
              </List>
            </Panel>
          );
        })}
      </Collapse>
    );
  }

  render() {
    if (this.state.quiz_loading || (this.props.delete_loading && this.state.quiz_id === this.props.delete_id)) {
      return <Loader />;
    }
    return (
      <>
        <div className={"quiz-detail"}>{this.renderQuestions()}</div>
        {this.state.showPreviewer && (
          <PreviewerComponent
            onClose={this.closePreviewer}
            onDownload={this.handleFileDownload}
            list={this.state.previewList}
            currentIndex={this.state.currentPreviewIndex}
          />
        )}
      </>
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(QuizDetailComponent);
