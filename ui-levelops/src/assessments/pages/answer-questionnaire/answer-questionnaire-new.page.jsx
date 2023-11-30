import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import queryString from "query-string";
import LocalStoreService from "services/localStoreService";
import { SelectRestapi } from "shared-resources/helpers";
import { getData, getError, getErrorCode, getLoading, loadingStatus } from "utils/loadingUtils";
import { RestQuiz } from "classes/RestQuiz";
import {
  mapRestapiDispatchtoProps,
  mapRestapiStatetoProps,
  mapWorkspaceStateToProps
} from "reduxConfigs/maps/restapiMap";
import { buildApiUrl, FILE_UPLOAD } from "constants/restUri";
import { buildLink } from "utils/integrationUtils";
import Loader from "components/Loader/Loader";
import {
  AntButton,
  AntCard,
  AntCol,
  AntModal,
  AntProgress,
  AntRow,
  AntText,
  AntTitle,
  SvgIcon
} from "shared-resources/components";
import { Checkbox, Divider, Icon, notification, Form } from "antd";
import ErrorWrapper from "hoc/errorWrapper";
import ConfirmationWrapper from "hoc/confirmationWrapper";
import { DownloadAssessment, Questions } from "assessments/components";
import "./aswer-questionnaire.style.scss";
import FileSaver from "file-saver";
import { getFileBlob, pdfBlob } from "assessments/utils";
import JSZip from "jszip";
import { get } from "lodash";
import { getReportsPage } from 'constants/routePaths'
import { AvatarWithText, NameAvatar, NameAvatarList } from "../../../shared-resources/components";
import { GenericMentions } from "../../../shared-resources/containers";
import { getWorkitemDetailPage } from "../../../constants/routePaths";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { AttachmentItem } from "../../../shared-resources/components/attachment-item/attachment-item.component";
import PreviewerComponent from "../../../shared-resources/components/previewer/previewer";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";

export class NewAnswerQuestionnairePage extends React.Component {
  constructor(props) {
    super(props);
    const ls = new LocalStoreService();
    const values = queryString.parse(this.props.location.search);
    let quizId = values.questionnaire;
    if (quizId === undefined) {
      quizId = "0";
      this.props.history.push(getReportsPage());
    } else {
      this.props.quizGet(quizId);
      this.props.genericList("users", "list", {}, null, "mentions_search");
    }
    this.state = {
      quiz_id: quizId,
      questionnaire_notification_loading: false,
      userRecipientsModalSheet: false,
      users: [],
      quiz_loading: quizId !== "0",
      quiz_submit_loading: false,
      quiz_files_uploading: false,
      kbs_loading: false,
      kb_files: [],
      preview_index: undefined,
      user_email: ls.getUserEmail(),
      tenant: ls.getUserCompany(),
      quiz: undefined,
      files_downloading: false,
      file_download_ids: [],
      file_uploads: [],
      file_report_ids: [],
      files_loading: false,
      display_completed_modal: false,
      report_ready: false,
      product_loading: false,
      product_loaded: false,
      product_error: false,
      vanity_id: null,
      work_item_loading: false,
      assignees: undefined,
      dirty_section: undefined,
      dirty_question: undefined,
      dirty_timestamp: undefined,
      resolve_conflict_loading: false,
      include_artifacts: true,
      expand_all_comments: false
    };

    this.printRef = React.createRef();

    this.buildArtifactLink = this.buildArtifactLink.bind(this);
    this.buildQuestions = this.buildQuestions.bind(this);
    this.buildFileUploadLink = this.buildFileUploadLink.bind(this);
    this.downloadFile = this.downloadFile.bind(this);
    this.previewFile = this.previewFile.bind(this);
    this.getPreviewFileData = this.getPreviewFileData.bind(this);
    this.getAssertion = this.getAssertion.bind(this);
    this.calculateTotalRiskScore = this.calculateTotalRiskScore.bind(this);
    this.answerQuiz = this.answerQuiz.bind(this);
    this.downloadQuiz = this.downloadQuiz.bind(this);
    this.handlePdf = this.handlePdf.bind(this);
    this.previewFile = this.previewFile.bind(this);
    this.toggleComments = this.toggleComments.bind(this);
    this.sendBySlack = this.sendBySlack.bind(this);
    this.sendRecipientsList = this.sendRecipientsList.bind(this);
  }

  componentWillUnmount() {
    this.props.restapiClear("quiz", "get", "-1");
    this.props.restapiClear("files", "head", "-1");
    this.props.restapiClear("files", "get", "-1");
    this.props.restapiClear("bestpractices", "get", "-1");
    this.props.restapiClear("quiz", "upload", "-1");
  }

  static getDerivedStateFromProps(props, state) {
    if (state.questionnaire_notification_loading) {
      const loading = get(props.rest_api, ["questionnaires_notify", "list", "0", "loading"], true);
      const error = get(props.rest_api, ["questionnaires_notify", "list", "0", "error"], true);
      if (!loading) {
        if (!error) {
          notification.success({
            message: "Questionnaire posted successfully"
          });
        } else {
          notification.error({
            message: "Failed to post questionnaire"
          });
        }
        return {
          ...state,
          questionnaire_notification_loading: false
        };
      }
    }

    if (state.quiz_loading) {
      if (
        !getLoading(props.rest_api, "quiz", "get", state.quiz_id) &&
        !getError(props.rest_api, "quiz", "get", state.quiz_id)
      ) {
        const data = getData(props.rest_api, "quiz", "get", state.quiz_id);
        let quiz = new RestQuiz(data);
        props.restapiLoading(true, "quiz", "get", state.quiz_id);
        quiz.kb_ids.forEach(kb => props.bpsGet(kb));
        return {
          ...state,
          quiz_loading: false,
          quiz: quiz,
          kbs_loading: quiz.kb_ids && quiz.kb_ids.length > 0
        };
      }
    }

    if (state.kbs_loading) {
      let loading = false;
      const kbs = [];
      state.quiz.kb_ids.forEach(kb => {
        const kbLoading = get(props.rest_api, ["bestpractices", "get", kb, "loading"], true);
        const kbError = get(props.rest_api, ["bestpractices", "get", kb, "error"], false);
        loading = loading || kbLoading;
        if (!kbLoading && !kbError) {
          const kbObj = get(props.rest_api, ["bestpractices", "get", kb, "data"], {});
          kbs.push(kbObj);
        }
      });
      return {
        ...state,
        kbs_loading: loading,
        kb_files: kbs
      };
    }

    if (state.resolve_conflict_loading) {
      if (
        !getLoading(props.rest_api, "quiz", "get", state.quiz_id) &&
        !getError(props.rest_api, "quiz", "get", state.quiz_id)
      ) {
        const data = getData(props.rest_api, "quiz", "get", state.quiz_id);
        let quiz = new RestQuiz(data);
        props.restapiLoading(true, "quiz", "get", state.quiz_id);

        console.log(state.dirty_timestamp);
        const currentAssert = quiz.assertion(state.dirty_section, state.dirty_question);
        console.log(currentAssert);
        if (state.dirty_timestamp && currentAssert && currentAssert.created_at !== state.dirty_timestamp) {
          // this question has changed, either in comments or responses
          notification.error({
            message: "Assessment update conflict",
            description: "Question has been updated by someone else. Please refresh your assessment"
          });
          return {
            ...state,
            quiz_submit_loading: false,
            dirty_section: undefined,
            dirty_question: undefined,
            dirty_timestamp: undefined,
            resolve_conflict_loading: false
          };
        } else {
          // update current dirty assertion to newQuiz and resubmit
          console.log("There is probably no conflict");
          if (state.dirty_timestamp && state.dirty_section && state.dirty_question) {
            console.log("Reconciling the two versions");
            const dirtyAssert = state.quiz.assertion(state.dirty_section, state.dirty_question);
            quiz.updateAssertion(state.dirty_section, state.dirty_question, dirtyAssert);
          } else {
            // maybe conflict in comments?
            if (state.quiz.comments !== quiz.comments) {
              notification.error({
                message: "Assessment update conflict",
                description: "Question has been updated by someone else. Please refresh your assessment"
              });
              return {
                ...state,
                quiz_submit_loading: false,
                dirty_section: undefined,
                dirty_question: undefined,
                dirty_timestamp: undefined,
                resolve_conflict_loading: false
              };
            } else {
              // something else we cant quite figure out
              notification.error({
                message: "Assessment update conflict",
                description: "Question has been updated by someone else. Please refresh your assessment"
              });
              return {
                ...state,
                quiz_submit_loading: false,
                dirty_section: undefined,
                dirty_question: undefined,
                dirty_timestamp: undefined,
                resolve_conflict_loading: false
              };
            }
          }
          console.log("sending out updated update");
          props.quizUpdate(quiz.id, quiz);
          return {
            ...state,
            quiz: quiz,
            quiz_submit_loading: true,
            resolve_conflict_loading: false
          };
        }
      }
    }

    if (!state.quiz_loading && state.quiz && !state.vanity_id && !state.work_item_loading && state.quiz.work_item_id) {
      props.workItemGet(state.quiz.work_item_id);
      return {
        ...state,
        work_item_loading: true
      };
    }

    if (!state.quiz_loading && state.work_item_loading && state.quiz) {
      let vanity_id;
      let assignees;
      const { loading, error } = loadingStatus(props.rest_api, "workitem", "get", state.quiz.work_item_id);
      if (!loading && !error) {
        const workItem = getData(props.rest_api, "workitem", "get", state.quiz.work_item_id);
        if (workItem) {
          vanity_id = workItem.vanity_id;
          assignees = workItem.assignees;
        }
        return {
          ...state,
          work_item_loading: false,
          vanity_id,
          assignees
        };
      }
    }

    if (state.quiz_files_uploading) {
      let loading = false;
      let quiz = state.quiz;
      state.file_uploads.forEach(file => {
        const uploadId = `${state.quiz.id}:${file.question_id}:${file.index}`;
        if (
          !getLoading(props.rest_api, "quiz", "upload", uploadId) &&
          !getError(props.rest_api, "quiz", "upload", uploadId)
        ) {
          const fileId = props.rest_api.quiz.upload[uploadId].data.id;
          for (let i = 0; i < quiz.section_responses.length; i++) {
            if (file.section_id.toString() === quiz.section_responses[i].section_id.toString()) {
              for (let j = 0; j < quiz.section_responses[i].answers.length; j++) {
                if (quiz.section_responses[i].answers[j].question_id.toString() === file.question_id.toString()) {
                  quiz.section_responses[i].answers[j].responses[file.index].value = fileId;
                  quiz.section_responses[i].answers[j].responses[file.index].upload = false;
                  break;
                }
              }
              break;
            }
          }
        } else {
          loading = true;
        }
      });
      if (!loading) {
        //quiz.completed = true;
        props.quizUpdate(quiz.id, quiz);
        return {
          ...state,
          quiz: quiz,
          quiz_files_uploading: false,
          quiz_submit_loading: true
        };
      }
    }

    if (state.quiz_submit_loading) {
      const loading = getLoading(props.rest_api, "quiz", "update", state.quiz.id);
      const error = getError(props.rest_api, "quiz", "update", state.quiz.id);
      if (!loading && !error) {
        // props.quizGet(state.quiz_id);
        // notification.success({
        //   message: "Success",
        //   description: "Questionnaire was submitted successfully"
        // });

        const data = getData(props.rest_api, "quiz", "update", state.quiz.id);
        const newQuiz = new RestQuiz(data);
        props.restapiClear("quiz", "update", state.quiz.id);
        return {
          ...state,
          quiz: newQuiz,
          quiz_submit_loading: false,
          dirty_question: undefined,
          dirty_section: undefined,
          dirty_timestamp: undefined
          //quiz_loading: true
        };
      } else if (!loading && error) {
        const errorCode = getErrorCode(props.rest_api, "quiz", "update", state.quiz.id);
        if (errorCode === 409) {
          props.quizGet(state.quiz.id);
          return {
            ...state,
            quiz_submit_loading: false,
            resolve_conflict_loading: true
          };
        }
        notification.error({
          message: "Assessment was not updated"
        });
        props.quizGet(state.quiz_id);
        return {
          ...state,
          quiz_submit_loading: false,
          quiz_loading: true,
          dirty_section: undefined,
          dirty_question: undefined,
          dirty_timestamp: undefined
        };
      }
    }

    if (state.files_loading) {
      let filesLoading = false;
      state.file_report_ids.forEach(obj => {
        const loading = getLoading(props.rest_api, "files", "get", obj.id);
        const error = getError(props.rest_api, "files", "get", obj.id);
        if (loading) {
          filesLoading = true;
        } else {
          const file = props.rest_api.files.get[obj.id].data;
          if (!error) {
            if (file === undefined) {
              filesLoading = true;
            }
          }
        }
      });
      return {
        ...state,
        files_loading: filesLoading,
        report_ready: !filesLoading
      };
    }

    if (state.report_ready) {
      let zip = new JSZip();
      let folder = zip.folder("assessments");
      (async () => {
        const quiz = state.quiz.json();
        const quizPDF = <DownloadAssessment assessment={quiz} />;
        const fileName = state.vanity_id ? `${state.vanity_id}_${quiz.qtemplate_name}` : quiz.qtemplate_name;
        folder.file(`${fileName}.pdf`, pdfBlob(quizPDF));
      })();
      state.file_report_ids.forEach(obj => {
        folder.file(obj.name, getFileBlob(props.rest_api.files.get[obj.id].data));
      });
      zip.generateAsync({ type: "blob" }).then(content => FileSaver.saveAs(content, "assessments.zip"));
      return {
        ...state,
        report_ready: false,
        file_report_ids: []
        //selected_ids: []
      };
    }

    if (state.product_loading) {
      // const { loading, error } = loadingStatus(props.rest_api, "products", "get", state.quiz.product_id);
      const workspace = props.workspace(state.quiz.product_id);
      const loading = get(workspace, ["loading"], true);
      const error = get(workspace, ["error"], false);

      return {
        product_loading: loading,
        product_loaded: !loading && !error,
        product_error: !loading && error
      };
    }

    if (state.file_downloading) {
      const fileIds = state.file_download_ids.map(f => f.file_id);
      if (fileIds.length) {
        return fileIds.reduce(
          (acc, fileId) => {
            const { loading, error } = loadingStatus(props.rest_api, "files", "get", fileId);
            if (!loading && !error) {
              const newIds = acc.file_download_ids.filter(file => file.file_id !== fileId);
              return {
                ...acc,
                file_download_ids: newIds
              };
            }

            return acc;
          },
          { ...state }
        );
      }

      return {
        ...state,
        file_downloading: false
      };
    }

    return null;
  }

  calculateTotalRiskScore() {
    let total_score = 0;
    let totalAnswered = 0;
    this.state.quiz.section_responses.forEach(answer => {
      answer.answers.forEach(assertion => {
        if (assertion.answered) {
          totalAnswered = totalAnswered + 1;
          if (Array.isArray(assertion.responses)) {
            assertion.responses.forEach(res => {
              total_score = total_score + parseInt(res.score);
            });
          } else {
            total_score = total_score + parseInt(assertion.responses.score);
          }
        }
      });
    });

    return { score: total_score, answered: totalAnswered };
  }

  answerQuiz(e) {
    let quiz = this.state.quiz;
    quiz.completed = e && e.currentTarget.id === "submit-quiz";
    quiz.user_email = this.state.user_email;
    let result = this.calculateTotalRiskScore();
    quiz.current_score = result.score;
    quiz.answered_questions = result.answered;
    let file_uploads = [];
    // determine if any files need to be uploaded and upload them here, wait for the file id in the props
    quiz.section_responses.forEach(answer => {
      answer.answers.forEach(assertion => {
        if (assertion.answered && assertion.responses !== undefined && assertion.responses[0] !== undefined) {
          assertion.responses.forEach((response, index) => {
            if (response.hasOwnProperty("type") && response.type === "file" && response.upload === true) {
              // use the file upload prop here and then add it to the expected file uploads array
              this.props.quizFileUpload(`${quiz.id}:${assertion.question_id}:${index}`, response.value);
              file_uploads.push({
                question_id: assertion.question_id,
                file: response.value,
                section_id: answer.section_id,
                index: index
              });
            }
          });
        }
      });
    });

    console.log(file_uploads);
    if (file_uploads.length === 0) {
      this.props.quizUpdate(this.state.quiz.id, this.state.quiz);
    }
    this.setState(
      {
        quiz: quiz,
        file_uploads: file_uploads,
        quiz_files_uploading: file_uploads.length > 0,
        quiz_submit_loading: file_uploads.length === 0
      },
      () => {
        this.props.setDirty(false);
      }
    );
  }

  sendBySlack(data) {
    this.setState({ userRecipientsModalSheet: true });
  }

  sendRecipientsList() {
    const questionnairedata = {
      questionnaire_id: this.state.quiz_id,
      recipients: [...this.state.user.map(user => user.label)],
      mode: "SLACK",
      requestor_type: "USER",
      requestor_id: this.props.session_user_id,
      requestor_name: this.props.session_first_name
    };
    notification.info({
      message: "Posting questionnaire..."
    });
    this.props.qsNotify(questionnairedata);
    this.setState({ userRecipientsModalSheet: false, questionnaire_notification_loading: true, user: [] });
  }

  getAssertion(questionId, assertionId) {
    for (let i = 0; i < this.state.quiz.section_responses.length; i++) {
      if (this.state.quiz.section_responses[i].section_id.toString() === questionId.toString()) {
        let answer = this.state.quiz.section_responses[i];
        for (let j = 0; j < answer.answers.length; j++) {
          if (answer.answers[j].question_id.toString() === assertionId.toString()) {
            return answer.answers[j];
          }
        }
      }
    }
    return null;
  }

  buildFileUploadLink(questionId, assertionId) {
    let assertion = this.getAssertion(questionId, assertionId);
    let fileId = assertion.responses[0].value;
    let fileName = assertion.responses[0].file_name;
    let uploadNeeded = assertion.upload;
    if (uploadNeeded || uploadNeeded === undefined) {
      console.log("not returning href");
      return "";
    } else {
      let fetchFileId = `quiz/${this.state.quiz.id}/assertion/${assertionId}/${fileId}`;
      // check if download button has already been clicked for this

      let url = buildApiUrl(`/${FILE_UPLOAD}/${fileId}`);
      return (
        <a href={url} id={fileId} onClick={e => this.downloadFile(e, fetchFileId, fileName)}>
          {fileName === null || fileName === undefined ? "Download file" : `Download ${fileName}`}
        </a>
      );
    }
  }

  downloadFile(e, fileId, fileName) {
    e.preventDefault();
    if (this.state.file_download_ids.findIndex(f => f.file_id === fileId) !== -1) {
      return;
    }
    let downloadIds = this.state.file_download_ids;
    downloadIds.push({ file_id: fileId, file_name: fileName });
    this.setState(
      {
        file_downloading: true,
        file_download_ids: downloadIds
      },
      () => {
        this.props.filesGet(fileId, fileName, true);
      }
    );
  }

  previewFile(fileId) {
    const { loading, error, data } = this.getPreviewFileData(fileId);

    if (!loading && !error && !data) {
      this.props.filesHead(fileId);
    }
  }

  getPreviewFileData(fileId) {
    return get(this.props.rest_api, ["files", "head", fileId], {
      loading: false,
      error: false,
      data: null
    });
  }

  buildArtifactLink(quiz) {
    if (
      quiz.integration_url === null ||
      quiz.integration_application === null ||
      quiz.integration_url === undefined ||
      quiz.integration_application === undefined
    ) {
      return null;
    }
    return (
      <a
        target="_blank"
        rel="noopener noreferrer"
        className={`${this.props.className}__artifact_link`}
        href={buildLink(quiz.artifact, quiz.integration_url, quiz.integration_application)}>
        {quiz.artifact || ""}
      </a>
    );
  }

  buildQuestions(quiz) {
    return quiz.sections.map(section => (
      <AntCol span={24}>
        <AntCard>
          <AntRow type={"flex"} justify={"space-between"} gutter={[20, 20]}>
            <AntCol span={6}>
              <div>
                <AntTitle level={4}>{section.name}</AntTitle>
              </div>
              <div>
                <AntText type={"secondary"} className="word-break-all">
                  {section.description}
                </AntText>
              </div>
            </AntCol>
            <AntCol span={17}>
              <Questions
                sectionId={section.id}
                quiz={this.state.quiz}
                fileDownloadingIds={this.state.file_download_ids}
                updateQuiz={(updatedQuiz, sectionId, questionId, should = true, prevTimeStamp = undefined) => {
                  const preUpdateAssertion = this.state.quiz.assertion(sectionId, questionId);
                  console.log(preUpdateAssertion);
                  this.setState(
                    {
                      quiz: updatedQuiz,
                      dirty_section: sectionId,
                      dirty_question: questionId,
                      dirty_timestamp: prevTimeStamp
                    },
                    () => {
                      if (should) {
                        this.answerQuiz();
                      }
                    }
                  );
                  this.props.setDirty(true);
                }}
                downloadFile={this.downloadFile}
                expandComments={this.state.expand_all_comments}
                previewFile={this.previewFile}
                getPreviewFileData={this.getPreviewFileData}
                filesDelete={this.props.filesDelete}
                disabled={
                  this.state.quiz_submit_loading || this.state.resolve_conflict_loading
                    ? this.state.dirty_question
                    : false
                }
              />
            </AntCol>
          </AntRow>
        </AntCard>
      </AntCol>
    ));
  }

  downloadQuiz() {
    //let pageHTML = document.documentElement.outerHTML;
    // const quiz = this.props.rest_api.quiz.get[this.state.quiz_id].data;
    // //const htmlString = renderToString(<DownloadAssessment assessment={quiz}/>);
    // const htmlString = ReactDOMServer.renderToStaticMarkup(<DownloadAssessment assessment={quiz}/>);
    // const file = new Blob([htmlString], {type: 'text/plain'});
    // FileSaver.saveAs(file, "assessment.htm");
  }

  handlePdf() {
    const filesToDownload = [
      {
        id: this.state.quiz.id,
        include_artifacts: this.state.include_artifacts
      }
    ];
    this.props.genericList("assessment_download", "list", filesToDownload, null, "0");
    // let filesLoading = false;
    // let fileIds = [];
    // if (this.state.include_artifacts) {
    //   quiz.section_responses.forEach(section => {
    //     section.answers.forEach(answer => {
    //       if (answer.answered && answer.responses.length > 0) {
    //         answer.responses.forEach(response => {
    //           if (response.type === "file") {
    //             // kick off downloading the file here
    //             filesLoading = true;
    //             let fetchFileId = `quiz/${quiz.id}/assertion/${answer.question_id}/${response.value}`;
    //             fileIds.push({
    //               id: fetchFileId,
    //               name: response.file_name
    //             });
    //             this.props.filesGet(fetchFileId, response.file_name, false);
    //           }
    //         });
    //       }
    //     });
    //   });
    // }
    // this.setState({
    //   report_ready: !filesLoading,
    //   files_loading: filesLoading,
    //   file_report_ids: fileIds
    // });
  }

  get productTitle() {
    if (!this.state.quiz.product_id) {
      // Should never happen. Just a failsafe.
      return null;
    }

    const {
      product_loading: productLoading,
      product_loaded: productLoaded,
      product_error: productError,
      quiz: { product_id: productId }
    } = this.state;

    if (!productLoading && !productLoaded && !productError) {
      // Send request to load product once.
      this.props.workspace(productId);
      this.setState({
        product_loading: true
      });

      // Show loader for the first time when sending the request...
      return <Loader />;
    }

    if (productLoading) {
      return <Loader />;
    }

    if (productError) {
      return "Unknown";
    }
    const workspace = this.props.workspace(productId);
    return get(workspace, ["data", "name"], "");
  }

  renderAssignees() {
    const { assignees } = this.state;
    if (assignees.length === 1) {
      return assignees.map(user => <AvatarWithText text={user.user_email} />);
    } else if (assignees.length > 1) {
      return <NameAvatarList names={assignees.map(assignee => assignee.user_email)} classRequired={false} />;
    } else {
      return "UNASSIGNED";
    }
  }

  previewFile(index) {
    console.log(`preview file ${index}`);
    this.setState({ preview_index: index });
  }

  toggleComments() {
    this.setState({ expand_all_comments: !this.state.expand_all_comments });
  }

  render() {
    if (
      this.state.quiz_loading ||
      //this.state.work_item_loading ||
      //this.state.quiz_submit_loading ||
      //this.state.quiz_files_uploading ||
      this.state.quiz_id === "0"
    ) {
      return <Loader />;
    }

    const { className } = this.props;
    //const quiz = this.props.rest_api.quiz.get[this.state.quiz_id].data;
    let progressValue = Math.ceil((this.state.quiz.answered_questions / this.state.quiz.total_questions) * 100);

    return (
      <div>
        <AntModal
          title="Recipients"
          visible={this.state.userRecipientsModalSheet}
          width={700}
          destroyOnClose={true}
          onCancel={() => {
            this.setState({ userRecipientsModalSheet: false, user: [] });
          }}
          footer={[
            <AntButton
              key="back"
              onClick={() => {
                this.setState({ userRecipientsModalSheet: false, user: [] });
              }}>
              Cancel
            </AntButton>,
            <AntButton
              key="submit"
              type="primary"
              disabled={!this.state.user || this.state?.user.length === 0}
              onClick={this.sendRecipientsList}>
              Send
            </AntButton>
          ]}>
          <Form.Item label="Select Recipients" colon={false}>
            <SelectRestapi
              style={{ width: "100%" }}
              value={this.state.user}
              mode="multiple"
              labelInValue
              rest_api={this.props.rest_api}
              fetchData={this.props.usersList}
              createOption={true}
              uri="users"
              searchField="email"
              onChange={Recipient => {
                this.setState({ user: Recipient });
              }}
            />
          </Form.Item>
        </AntModal>

        {this.state.preview_index !== undefined && (
          <PreviewerComponent
            onClose={() => this.setState({ preview_index: undefined })}
            onDownload={this.downloadFile}
            list={this.state.kb_files
              .filter(k => k.type === "FILE")
              .map(kbFile => {
                let fetchFileId = `kb/${kbFile.id}/${kbFile.value}`;

                return {
                  fileId: kbFile.value,
                  fileName: kbFile.metadata || "download",
                  fetchFileId: fetchFileId
                };
              })}
            currentIndex={this.state.preview_index}
          />
        )}
        <AntRow gutter={[20, 20]}>
          <AntCol span={24}>
            <AntCard>
              <div>
                <SvgIcon icon={"levelops"} style={{ height: "4rem", width: "4rem" }} />
                <AntText type={"secondary"}>Powered by SEI</AntText>
              </div>
              <AntRow type={"flex"} justify={"space-between"} align={"bottom"}>
                <AntCol>
                  <AntTitle level={4} copyable={{ text: `${window.location.href}&tenant=${this.state.tenant}` }}>
                    {this.state.quiz.qtemplate_name}
                  </AntTitle>
                </AntCol>
                <AntCol>
                  <div>
                    <AntButton
                      type={"secondary"}
                      id={"submit-quiz"}
                      onClick={this.sendBySlack}
                      style={{ marginRight: "0.5rem" }}>
                      Send by Slack
                    </AntButton>
                    <AntButton type={"primary"} id={"submit-quiz"} onClick={this.answerQuiz}>
                      {this.state.quiz.completed ? "Re-Submit" : "Submit For Review"}
                    </AntButton>
                  </div>
                </AntCol>
              </AntRow>
              <AntRow>
                <AntCol>
                  <Checkbox
                    checked={this.state.include_artifacts}
                    onChange={e => this.setState({ include_artifacts: e.target.checked })}>
                    Include Artifacts
                  </Checkbox>
                  <AntButton icon={"download"} onClick={this.handlePdf}>
                    Download Assessment
                  </AntButton>
                </AntCol>
              </AntRow>
              <Divider />
              <AntRow type={"flex"} justify={"space-between"} align={"top"}>
                <AntCol span={2}>
                  <AntProgress strokeWidth={10} percent={progressValue} type={"circle"} width={50} />
                </AntCol>
                <AntCol span={4}>
                  <div className={`${className}__label`}>Artifact</div>
                  <div className={`${className}__artifact_container`}>
                    {this.buildArtifactLink(this.state.quiz)}
                    {this.state.vanity_id && (
                      <a
                        target="_blank"
                        rel="noopener noreferrer"
                        href={`${getWorkitemDetailPage()}?workitem=${this.state.vanity_id}`}
                        className={`${this.props.className}__artifact_link`}>
                        {this.state.vanity_id}
                      </a>
                    )}
                  </div>
                </AntCol>
                <AntCol span={5}>
                  <div className={`${className}__label`}>Sent To</div>
                  <div className={`${this.props.className}__info-value`}>
                    {this.state.assignees && this.renderAssignees()}
                  </div>
                </AntCol>
                <AntCol span={6}>
                  <div className={`${className}__label`}>Sent By</div>
                  <div className={`${this.props.className}__info-value`}>
                    <AvatarWithText text={this.state.quiz.sender_email} classRequired={false} />
                    {/*{this.state.quiz.sender_email}*/}
                  </div>
                </AntCol>
                {this.state.quiz.product_id && (
                  <AntCol span={4}>
                    <div className={`${className}__label`}>PROJECT</div>
                    <div className={`${this.props.className}__info-value`}>{this.productTitle}</div>
                  </AntCol>
                )}
                <AntCol span={3}>
                  <AntButton onClick={this.toggleComments}>
                    {this.state.expand_all_comments ? "Collapse All Comments" : "Expand All Comments"}
                  </AntButton>
                </AntCol>
              </AntRow>
              {this.state.quiz.kb_ids && this.state.quiz.kb_ids.length > 0 && (
                <>
                  <Divider />
                  <AntRow type={"flex"} justify={"start"} align={"top"}>
                    <AntCol span={24}>
                      <div className={`${className}__label`}>Reference Documents</div>
                      <div className={`${this.props.className}__info-value`}>
                        {this.state.kb_files
                          .filter(k => k.type === "LINK")
                          .map(link => (
                            <AntRow gutter={[10, 10]}>
                              <AntCol span={1}>
                                <Icon type={"link"} style={{ fontSize: "14px", marginLeft: "5px" }} />
                              </AntCol>
                              <AntCol span={23}>
                                <a href={link.value} target={"_blank"} rel="noopener noreferrer">
                                  {link.name}
                                </a>
                              </AntCol>
                            </AntRow>
                          ))}
                        {this.state.kb_files
                          .filter(k => k.type === "FILE")
                          .map((kbFile, index) => (
                            <AttachmentItem
                              fileName={kbFile.metadata || "download"}
                              previewFile={() => this.previewFile(index)}
                            />
                          ))}
                      </div>
                    </AntCol>
                  </AntRow>
                </>
              )}
            </AntCard>
          </AntCol>
          <div>{this.buildQuestions(this.state.quiz)}</div>
          <AntCol span={24}>
            <AntCard>
              <AntRow type={"flex"} justify={"start"} gutter={[40, 40]} align={"top"}>
                <AntCol span={7}>
                  <AntTitle level={4}>Comments</AntTitle>
                </AntCol>
                <AntCol span={12}>
                  <GenericMentions
                    placeholder="Add a comment...."
                    value={this.state.quiz.comments}
                    id={"comments"}
                    uri="users"
                    method="list"
                    searchField="email"
                    onChange={comment => {
                      let quiz = this.state.quiz;
                      quiz.comments = comment;
                      this.setState({ quiz: quiz });
                    }}
                    onBlur={e => this.answerQuiz()}
                    optionRenderer={(user, email) => {
                      return (
                        <>
                          <NameAvatar name={email} />
                          {email}
                        </>
                      );
                    }}
                    optionValueTransformer={option => `[${option.email}]`}
                  />

                  {/*<AntInput*/}
                  {/*  type="textarea"*/}
                  {/*  autoSize={{ minRows: 3, maxRows: 5 }}*/}
                  {/*  id="quiz-comments"*/}
                  {/*  onChange={e => {*/}
                  {/*    let quiz = this.state.quiz;*/}
                  {/*    quiz.comments = e.currentTarget.value;*/}
                  {/*    this.setState({ quiz: quiz }, this.answerQuiz());*/}
                  {/*  }}*/}
                  {/*  value={this.state.quiz.comments}*/}
                  {/*/>*/}
                </AntCol>
                <AntCol span={4}>
                  <AntButton onClick={e => this.answerQuiz()} type={"primary"}>
                    Save Comment
                  </AntButton>
                </AntCol>
              </AntRow>
            </AntCard>
          </AntCol>
        </AntRow>
      </div>
    );
  }
}

NewAnswerQuestionnairePage.propTypes = {
  className: PropTypes.string
};

NewAnswerQuestionnairePage.defaultProps = {
  className: "answer-questionnaire"
};

const mapStateToProps = state => {
  return {
    ...mapRestapiStatetoProps(state),
    ...mapSessionStatetoProps(state),
    ...mapWorkspaceStateToProps(state)
  };
};

const mapDispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapGenericToProps(dispatch)
});
export default ErrorWrapper(
  connect(mapStateToProps, mapDispatchToProps)(ConfirmationWrapper(NewAnswerQuestionnairePage))
);
