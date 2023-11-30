import React from "react";
import * as PropTypes from "prop-types";
import { Divider, Icon, Input, Modal, Popconfirm, Upload } from "antd";
import {
  AntButton,
  AntCheckbox,
  AntCheckboxGroup,
  AntCol,
  AntInput,
  AntRadio,
  AntRadioGroup,
  AntRow,
  AntText,
  SvgIcon
} from "shared-resources/components";
import LocalStoreService from "services/localStoreService";
import { Comments } from "../index";
import { get } from "lodash";
import "./questions.style.scss";
import PreviewerComponent from "../../../shared-resources/components/previewer/previewer";
import { AttachmentItem } from "../../../shared-resources/components/attachment-item/attachment-item.component";

import moment from "moment";

export class QuestionsComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    const ls = new LocalStoreService();
    this.state = {
      tenant: ls.getUserCompany(),
      user_email: ls.getUserEmail(),
      show_comments: undefined,
      link_upload: {},
      text_value: {},
      cachedIds: {},
      showPreviewer: false,
      previewList: [],
      currentPreviewIndex: -1
    };
    this.showComments = this.showComments.bind(this);
    this.buildComments = this.buildComments.bind(this);
    this.updateComments = this.updateComments.bind(this);
    this.closePreviewer = this.closePreviewer.bind(this);
  }

  closePreviewer() {
    this.setState({
      showPreviewer: false,
      previewList: [],
      currentPreviewIndex: -1
    });
  }

  previewFile(sectionId, questionId, _fileId) {
    let assertion = this.getAssertion(sectionId, questionId);
    let currentIndex = -1;
    if (!assertion) {
      return;
    }

    const fileIds = assertion.responses
      .filter(f => f.type !== "link")
      .map((response, index) => {
        let fileId = response.value;
        let fileName = response.file_name;
        let uploadNeeded = response.upload;
        if (assertion.answered === false || uploadNeeded === true) {
          console.log("not returning href");
          return "";
        }

        let fetchFileId = `quiz/${this.props.quiz.id}/assertion/${questionId}/${fileId}`;

        return {
          fileId: fileId,
          fileName: fileName,
          fetchFileId: fetchFileId
        };
      });

    currentIndex = fileIds.findIndex(f => f.fileId === _fileId);

    if (currentIndex !== -1) {
      this.setState({
        previewList: fileIds,
        currentPreviewIndex: currentIndex,
        showPreviewer: true
      });
    }
  }

  getAssertion(sectionId, questionId) {
    for (let i = 0; i < this.props.quiz.section_responses.length; i++) {
      if (this.props.quiz.section_responses[i].section_id.toString() === sectionId.toString()) {
        let answer = this.props.quiz.section_responses[i];
        for (let j = 0; j < answer.answers.length; j++) {
          if (answer.answers[j].question_id.toString() === questionId.toString()) {
            return answer.answers[j];
          }
        }
      }
    }
    return null;
  }

  updateMultiSelect(sectionId, questionId, value) {
    let quiz = this.props.quiz;
    const updatedValues = value.map(val => JSON.parse(val));
    let prevCreatedAt = undefined;

    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
    answer.answers.forEach(assert => {
      if (assert.question_id === questionId) {
        prevCreatedAt = assert.created_at;
        assert.responses = updatedValues;
        assert.user_email = this.state.user_email;
        assert.created_at = moment().unix();
        assert.answered = true;
      }
    });

    quiz.section_responses.forEach(ans => {
      if (ans.section_id === sectionId.toString()) {
        ans.answers = answer.answers;
      }
    });
    this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  }

  updateChecklist(sectionId, questionId, value) {
    let quiz = this.props.quiz;
    const updatedValues = value.map(val => JSON.parse(val));
    let prevCreatedAt = undefined;
    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
    answer.answers.forEach(assert => {
      if (assert.question_id === questionId) {
        prevCreatedAt = assert.created_at;
        assert.responses = updatedValues;
        assert.user_email = this.state.user_email;
        assert.answered = true;
        assert.created_at = moment().unix();
      }
    });
    this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  }

  updateSingleSelect(e) {
    const { value, score, question, section, originalValue } = e.target;
    let quiz = this.props.quiz;
    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === section.toString())[0];
    let prevCreatedAt = undefined;
    answer.answers.forEach(assert => {
      if (assert.question_id === question) {
        prevCreatedAt = assert.created_at;
        assert.responses = [{ value: value, score: score, original_value: originalValue }];
        assert.user_email = this.state.user_email;
        assert.answered = true;
        assert.created_at = moment().unix();
      }
    });
    quiz.section_responses.forEach(ans => {
      if (ans.section_id.toString() === question) {
        ans.answers = answer.answers;
      }
    });
    this.props.updateQuiz(quiz, section, question, true, prevCreatedAt);
  }

  updateTextValue(e, sectionId, questionId, score, should) {
    const value = e.currentTarget.value;
    let quiz = this.props.quiz;
    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
    let prevCreatedAt = undefined;
    answer.answers.forEach(assert => {
      if (assert.question_id === questionId) {
        prevCreatedAt = assert.created_at;
        assert.responses = [{ value: value, score: score }];
        assert.user_email = this.state.user_email;
        assert.answered = value !== "";
        assert.created_at = moment().unix();
      }
    });
    quiz.section_responses.forEach(ans => {
      if (ans.section_id.toString() === sectionId.toString()) {
        ans.answers = answer.answers;
      }
    });
    this.props.updateQuiz(quiz, sectionId, questionId, should, prevCreatedAt);
  }

  handleFileUpload(file, sectionId, questionId, score, link = false) {
    let quiz = this.props.quiz;
    let answers = quiz.section_responses;
    let prevCreatedAt = undefined;
    for (let i = 0; i < answers.length; i++) {
      if (answers[i].section_id.toString() === sectionId.toString()) {
        for (let j = 0; j < answers[i].answers.length; j++) {
          if (answers[i].answers[j].question_id.toString() === questionId.toString()) {
            prevCreatedAt = answers[i].answers[j].created_at;
            answers[i].answers[j].user_email = file !== undefined ? this.state.user_email : undefined;
            answers[i].answers[j].created_at = file !== undefined ? moment().unix() : undefined;
            answers[i].answers[j].answered = file !== undefined;
            answers[i].answers[j].upload = file !== undefined && link === false ? true : undefined;
            if (link === true) {
              answers[i].answers[j].upload = false;
            }
            const responseLength = answers[i].answers[j].responses.length;
            const response =
              file !== undefined
                ? {
                    value: file,
                    file_name: file.name,
                    score: responseLength === 0 ? parseInt(score) : 0,
                    type: link === false ? "file" : "link",
                    upload: link === false,
                    upload_id: `${this.props.quiz.id}:${questionId}:${responseLength}`
                  }
                : {
                    value: "",
                    file_name: "",
                    type: "file",
                    score: 0,
                    upload: false
                  };
            answers[i].answers[j].responses.push(response);
          }
        }
      }
    }
    quiz.section_responses = answers;
    this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  }

  handleFileRemove(file, sectionId, questionId) {
    let quiz = this.props.quiz;
    let prevCreatedAt = undefined;
    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
    answer.answers.forEach(assert => {
      if (assert.question_id === questionId) {
        prevCreatedAt = assert.created_at;
        const newResponses = assert.responses.filter(response => get(response, ["value", "uid"], "0") !== file.uid);
        assert.created_at = moment().unix();
        assert.responses = newResponses;
      }
    });

    this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  }

  removeFile(sectionId, questionId, index, isLink) {
    let quiz = this.props.quiz;
    let prevCreatedAt = undefined;
    let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
    let fileId = undefined;
    console.log(`${sectionId} ${questionId} ${index} ${isLink}`);
    answer.answers.forEach(assert => {
      if (assert.question_id === questionId) {
        if (!isLink && assert.responses[index].type === "file") {
          fileId = `quiz/${this.props.quiz.id}/assertion/${questionId}/${assert.responses[index].value}`;
        }
        prevCreatedAt = assert.created_at;
        assert.created_at = moment().unix();

        assert.responses.splice(index, 1);
        if (assert.responses.length === 0) {
          assert.answered = false;
        }
      }
    });

    if (fileId) {
      this.props.filesDelete(fileId);
    }

    this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  }

  buildFileUploadLink(sectionId, questionId) {
    let assertion = this.getAssertion(sectionId, questionId);
    if (!assertion) {
      return "";
    }
    const links = assertion.responses
      .filter(f => f.type === "link")
      .map((response, index) => {
        let fileId = response.value;
        let uploadNeeded = response.upload;
        if (assertion.answered === false || uploadNeeded === true) {
          console.log("not returning href");
          return null;
        }
        return (
          <AntRow className={"upload-list"}>
            <AntCol span={1}>
              <Icon type={"link"} style={{ marginLeft: "5px", fontSize: "16px" }} />
            </AntCol>
            <AntCol span={22}>
              <div
                style={{
                  fontSize: "14px",
                  whiteSpace: "no-wrap",
                  wordBreak: "any",
                  overflow: "hidden",
                  textOverflow: "ellipsis"
                }}>
                <a
                  href={fileId}
                  style={{
                    fontSize: "14px",
                    whiteSpace: "nowrap",
                    wordBreak: "any",
                    overflow: "hidden",
                    textOverflow: "ellipsis"
                  }}>
                  {fileId}
                </a>
              </div>
            </AntCol>
            <AntCol span={1}>
              <Popconfirm
                title={"Do you want to delete this item?"}
                onConfirm={() => this.removeFile(sectionId, questionId, index, true)}
                okText={"Yes"}
                cancelText={"No"}>
                <Icon type={"close"} className={"close-icon"} />
              </Popconfirm>
            </AntCol>
          </AntRow>
        );
      });

    const nonLinks = assertion.responses
      .filter(f => f.type !== "link")
      .map((response, index) => {
        let fileId = response.value;
        let fileName = response.file_name;
        let uploading = this.props.getFileUploading(response.upload_id);
        if (assertion.answered === false) {
          console.log("not returning href");
          return null;
        }
        return (
          <AttachmentItem
            fileName={fileName}
            loading={uploading}
            previewFile={() => this.previewFile(sectionId, questionId, fileId)}
            removeFile={() => this.removeFile(sectionId, questionId, index, false)}
          />
        );
      });
    return (
      <>
        {nonLinks}
        {links}
      </>
    );
  }

  updateOptionValue(sectionId, questionId, index) {
    return e => {
      const { quiz } = this.props;
      let section = quiz.sections.find(section => section.id === sectionId);
      let question = section.questions.filter(question => question.id === questionId)[0];
      question.options[index].updatedValue = e.currentTarget.value;
      this.props.updateQuiz(quiz, sectionId, questionId, false, undefined);
    };
  }

  updateComments(sectionId, questionId) {
    let prevCreatedAt = undefined;
    return comment => {
      const { quiz } = this.props;
      let answer = quiz.section_responses.filter(answer => answer.section_id.toString() === sectionId.toString())[0];
      answer.answers.forEach(assert => {
        if (assert.question_id === questionId) {
          let comments = assert.comments || [];
          comments.push(comment);
          prevCreatedAt = assert.created_at;
          assert.comments = comments;
          assert.created_at = moment().unix();
        }
      });
      quiz.section_responses.forEach(ans => {
        if (ans.section_id.toString() === sectionId.toString()) {
          ans.answers = answer.answers;
        }
      });
      this.props.updateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
    };
  }

  showComments(sectionId, questionId) {
    this.setState({ show_comments: { section_id: sectionId, question_id: questionId } });
  }

  buildComments() {
    const questionId = this.state.show_comments ? this.state.show_comments.question_id : undefined;
    const sectionId = this.state.show_comments ? this.state.show_comments.section_id : undefined;
    if (questionId && sectionId) {
      let assertion = this.getAssertion(sectionId, questionId);
      const comments = assertion.comments || [];
      return (
        <Modal
          title={"Comments"}
          onCancel={e => this.setState({ show_comments: undefined })}
          visible={this.state.show_comments !== undefined}
          onOk={e => this.setState({ show_comments: undefined })}
          footer={null}>
          <div className={"comment-wrapper"}>
            <Comments
              comments={comments}
              creator={this.state.user_email}
              onAddComment={this.updateComments(sectionId, questionId)}
            />
          </div>
        </Modal>
      );
    }

    return null;
  }

  buildAssertions(section) {
    let result = [];
    section.questions
      .sort((a, b) => {
        return a.number - b.number;
      })
      .forEach((question, index) => {
        let assertionField = "";
        const answer = this.getAssertion(section.id, question.id);
        let userEmail = "";
        let createdOn = null;
        let comments = answer.comments || [];
        if (answer) {
          userEmail = answer.answered ? answer.user_email : "";
          createdOn = answer.created_at ? moment.unix(answer.created_at).format("MMM DD, YYYY") : null;
        }

        switch (question.type.toLowerCase()) {
          case "checklist":
            const checklistValues = answer
              ? answer.responses.map(resp =>
                  JSON.stringify({
                    value: resp.value !== "" ? true : "",
                    score: resp.score
                  })
                )
              : [];
            assertionField = (
              <AntCheckboxGroup
                style={{ width: "100%" }}
                disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                value={checklistValues}
                onChange={value => {
                  this.updateChecklist(section.id, question.id, value);
                }}>
                <AntCheckbox
                  key={index}
                  value={JSON.stringify({
                    value: true,
                    score: question.options[0].score
                  })}>
                  {question.name}
                </AntCheckbox>
              </AntCheckboxGroup>
            );
            break;
          case "boolean":
            const value = answer ? get(answer, ["responses", 0, "value"], undefined) : undefined;
            assertionField = (
              <AntRadioGroup
                value={value}
                disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                onChange={e => {
                  this.updateSingleSelect(e);
                }}>
                {question.options.map((option, oi) => {
                  return (
                    <AntRadio
                      key={oi}
                      value={option.value}
                      originalValue={option.value}
                      score={option.score}
                      question={question.id}
                      section={section.id}>
                      {option.value}
                    </AntRadio>
                  );
                })}
              </AntRadioGroup>
            );
            break;
          case "text":
            const textValue = answer ? get(answer, ["responses", 0, "value"], "") : "";
            assertionField = (
              <AntInput
                disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                type="textarea"
                name={`assertion:text:${question.id}:${question.options[0].score}`}
                value={textValue}
                //defaultValue={textValue}
                onBlur={e => this.updateTextValue(e, section.id, question.id, question.options[0].score, true)}
                onChange={e => this.updateTextValue(e, section.id, question.id, question.options[0].score, false)}
                autoSize={{ minRows: 3, maxRows: 5 }}
              />
            );
            break;
          case "multi-select":
            const multiValues = answer
              ? answer.responses.map(resp =>
                  JSON.stringify({
                    value: resp.value,
                    score: resp.score,
                    original_value: resp.original_value || resp.value
                  })
                )
              : [];
            assertionField = (
              <AntCheckboxGroup
                disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                style={{ width: "100%" }}
                value={multiValues}
                onChange={value => {
                  this.updateMultiSelect(section.id, question.id, value);
                }}>
                <AntRow gutter={[20, 20]}>
                  {question.options.map((option, oi) => (
                    <AntCol span={24}>
                      <AntCheckbox
                        key={oi}
                        value={JSON.stringify({
                          value:
                            option.updatedValue !== undefined
                              ? option.updatedValue
                              : answer.responses.filter(resp => resp.original_value === option.value).length > 0
                              ? answer.responses.filter(resp => resp.original_value === option.value)[0].value
                              : option.value,
                          score: option.score,
                          original_value: option.value
                        })}>
                        {option.editable && (
                          <AntInput
                            value={
                              option.updatedValue !== undefined
                                ? option.updatedValue
                                : answer.responses.filter(resp => resp.original_value === option.value).length > 0
                                ? answer.responses.filter(resp => resp.original_value === option.value)[0].value
                                : option.value
                            }
                            style={{ width: 250 }}
                            onChange={this.updateOptionValue(section.id, question.id, oi)}
                          />
                        )}
                        {option.editable !== true && option.value}
                      </AntCheckbox>
                    </AntCol>
                  ))}
                </AntRow>
              </AntCheckboxGroup>
            );
            break;
          case "single-select":
            const singleValue = answer ? get(answer, ["responses", 0, "value"], undefined) : undefined;
            const originalValue = singleValue
              ? get(answer, ["responses", 0, "original_value"], undefined) ||
                get(answer, ["responses", 0, "value"], undefined)
              : undefined;
            assertionField = (
              <AntRadioGroup
                value={singleValue}
                onChange={e => this.updateSingleSelect(e)}
                disabled={this.props.disabled !== false && this.props.disabled !== question.id}>
                <AntRow gutter={[20, 20]}>
                  {question.options.map((option, oi) => (
                    <AntCol span={24}>
                      <AntRadio
                        key={oi}
                        value={
                          option.updatedValue !== undefined
                            ? option.updatedValue
                            : singleValue && originalValue === option.value
                            ? singleValue
                            : option.value
                        }
                        originalValue={option.value}
                        score={option.score}
                        question={question.id}
                        section={section.id}>
                        {option.editable && (
                          <AntInput
                            disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                            value={
                              option.updatedValue !== undefined
                                ? option.updatedValue
                                : singleValue && originalValue === option.value
                                ? singleValue
                                : option.value
                            }
                            style={{ width: 250 }}
                            onChange={this.updateOptionValue(section.id, question.id, oi)}
                          />
                        )}
                        {option.editable !== true && (
                          <span style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>{option.value}</span>
                        )}
                      </AntRadio>
                    </AntCol>
                  ))}
                </AntRow>
              </AntRadioGroup>
            );
            break;
          case "file upload":
            const linkValue = this.state.link_upload[`${section.id}${question.id}`];
            assertionField = (
              <AntRow gutter={[10, 10]}>
                <AntCol>
                  <AntRow gutter={[0, 0]} type={"flex"} justify={"start"} align={"middle"}>
                    <AntCol span={6}>
                      <>
                        <Upload
                          disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                          multiple={true}
                          beforeUpload={(file, fileList) => {
                            this.handleFileUpload(file, section.id, question.id, question.options[0].score);
                            return false;
                          }}
                          onRemove={file => {
                            this.handleFileRemove(file, section.id, question.id);
                            //this.handleFileUpload(undefined, section.id, question.id, question.options[0].score);
                          }}
                          showUploadList={false}
                          // fileList={
                          //   answer.responses.filter(response => response.type === "file" && response.upload === true).map(
                          //       file => ({uid: file.file_name, name: file.file_name, status: "uploading"})
                          //   )
                          // }
                        >
                          <AntButton>
                            <Icon type="file" />
                            Upload File
                          </AntButton>
                        </Upload>
                      </>
                    </AntCol>
                    <AntCol span={18}>
                      <Input
                        disabled={this.props.disabled !== false && this.props.disabled !== question.id}
                        addonBefore={<Icon type="link" />}
                        placeholder={"Add a link here"}
                        allowClear={true}
                        value={linkValue}
                        onChange={e => {
                          let linkUpload = this.state.link_upload;
                          linkUpload[`${section.id}${question.id}`] = e.currentTarget.value;
                          this.setState({ link_upload: { ...linkUpload } });
                        }}
                        onPressEnter={e => {
                          e.preventDefault();
                          const value = e.currentTarget.value;
                          let linkUpload = this.state.link_upload;
                          linkUpload[`${section.id}${question.id}`] = "";
                          this.setState({ link_upload: { ...linkUpload } });
                          this.handleFileUpload(value, section.id, question.id, question.options[0].score, true);
                        }}
                      />
                    </AntCol>
                  </AntRow>
                </AntCol>
                <AntCol>{this.buildFileUploadLink(section.id, question.id)}</AntCol>
              </AntRow>
            );
            break;
          default:
            break;
        }

        const showComments =
          (this.state.show_comments &&
            this.state.show_comments.section_id === section.id &&
            this.state.show_comments.question_id === question.id) ||
          this.props.expandComments;

        result.push(
          <AntRow type={"flex"} justify={"space-between"} gutter={[20, 20]} key={index}>
            <AntCol span={24}>
              {question.type !== "checklist" && (
                <div style={{ margin: "10px" }}>
                  <AntText style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>{question.name}</AntText>
                </div>
              )}

              <div style={{ margin: "10px" }}>{assertionField}</div>
              {!!this.props.showAnsweredBy && (
                <AntRow
                  type={"flex"}
                  justify={"space-between"}
                  align={"bottom"}
                  style={{ margin: "10px", paddingTop: "10px" }}>
                  <AntCol span={4}>
                    <div
                      style={{ cursor: "pointer" }}
                      className={`flex direction-row`}
                      onClick={e =>
                        showComments
                          ? this.setState({ show_comments: undefined })
                          : this.showComments(section.id, question.id)
                      }>
                      <SvgIcon
                        icon={"comments"}
                        theme={comments.length > 0 ? "filled" : "outlined"}
                        className={comments.length === 0 ? "comment-icon" : "comment-icon__enabled"}
                      />
                      {comments.length > 0 && !showComments && <AntText>{comments.length} Comments</AntText>}
                      {showComments && <AntText>Hide Comments</AntText>}
                      {comments.length === 0 && !showComments && <AntText>Add Comment</AntText>}
                    </div>
                  </AntCol>
                  <AntCol span={20}>
                    {answer.answered && (
                      <div style={{ paddingBottom: "5px" }}>
                        <AntText type={"secondary"}>ANSWERED BY</AntText>
                        <AntText>
                          {" "}
                          {userEmail} on {createdOn}
                        </AntText>
                      </div>
                    )}
                  </AntCol>
                  {showComments && (
                    <div className={"comment-wrapper"}>
                      <Comments
                        comments={comments}
                        creator={this.state.user_email}
                        onAddComment={this.updateComments(section.id, question.id)}
                      />
                    </div>
                  )}
                </AntRow>
              )}
            </AntCol>
            {index !== section.questions.length - 1 && question.type !== "checklist" && <Divider />}
          </AntRow>
        );
      });
    return result;
  }

  render() {
    const { sectionId, quiz } = this.props;
    const section = quiz.sections.filter(section => section.id === sectionId)[0];
    return (
      <div>
        {/*{this.state.show_comments !== undefined && this.buildComments()}*/}
        {this.buildAssertions(section)}
        {this.state.showPreviewer && (
          <PreviewerComponent
            onClose={this.closePreviewer}
            onDownload={this.props.downloadFile}
            list={this.state.previewList}
            currentIndex={this.state.currentPreviewIndex}
          />
        )}
      </div>
    );
  }
}

QuestionsComponent.propTypes = {
  sectionId: PropTypes.string.isRequired,
  quiz: PropTypes.object.isRequired,
  updateQuiz: PropTypes.func.isRequired,
  downloadFile: PropTypes.func.isRequired,
  previewFile: PropTypes.func.isRequired,
  getPreviewFileData: PropTypes.func.isRequired,
  showAnsweredBy: PropTypes.bool,
  filesDelete: PropTypes.func,
  disabled: PropTypes.bool.isRequired,
  fileDownloadingIds: PropTypes.array.isRequired,
  getFileUploading: PropTypes.func.isRequired
};

QuestionsComponent.defaultProps = {
  showAnsweredBy: true,
  disabled: false
};
