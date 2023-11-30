import { notification } from "antd";
import { DownloadAssessment } from "assessments/components";
import AnswerQuestionnaireComponent from "assessments/components/answer-questionnaire/answer-questionnaire.component";
import RecepientsModal from "assessments/components/answer-questionnaire/recipientsModal.component";
import { getFileBlob, pdfBlob } from "assessments/utils";
import { getCalculatedRiskScore, QuestionsHandlerType } from "assessments/utils/helper";
import { RestQuiz } from "classes/RestQuiz";
import Loader from "components/Loader/Loader";
import { getReportsPage } from "constants/routePaths";
import FileSaver from "file-saver";
import ConfirmationWrapper, { ConfirmationWrapperProps } from "hoc/confirmationWrapper";
import JSZip from "jszip";
import { difference, filter, findIndex, forEach, get, isArray, map, uniqBy } from "lodash";
import queryString from "query-string";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import {
  bpsGet,
  filesDelete,
  filesGet,
  genericList,
  productsGet,
  qsNotify,
  quizFileUpload,
  quizGet,
  quizUpdate,
  restapiClear,
  workItemGet
} from "reduxConfigs/actions/restapi";
import { workspaceRead } from "reduxConfigs/actions/workspaceActions";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { getGenericMethodSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";
import PreviewerComponent from "shared-resources/components/previewer/previewer";

interface AnswerQuestionnaireContainerProps extends RouteComponentProps, ConfirmationWrapperProps {}

const AnswerQuestionnaireContainer: React.FC<AnswerQuestionnaireContainerProps> = (
  props: AnswerQuestionnaireContainerProps
) => {
  const values = queryString.parse(props.location.search);
  const { questionnaire: quizId } = values;

  const { setDirty } = props;

  const hasQuizId = quizId !== undefined && quizId !== null;

  const [quiz, setQuiz] = useState<RestQuiz | undefined>();
  const [quizLoading, setQuizLoading] = useState<boolean>(hasQuizId);
  const [product, setProduct] = useState<WorkspaceModel>();
  const [productLoading, setProductLoading] = useState<boolean>(false);
  const [workItem, setWorkItem] = useState<any>({});
  const [workItemLoading, setWorkItemLoading] = useState<boolean>(false);
  const [kbs, setKbs] = useState<any[]>([]);
  const [kbsLoading, setKbsLoading] = useState<boolean>(false);
  const [quizSubmitLoading, setQuizSubmitLoading] = useState<boolean>(false);
  const [dirtyQuestion, setDirtyQuestion] = useState<{ question: any; section: any; timestamp: any } | undefined>();
  const [resolveConflictLoading, setResolveConflictLoading] = useState<boolean>(false);
  const [fileUploads, setFileUploads] = useState<any[]>([]);
  const [filesLoading, setFilesLoading] = useState<boolean>(false);
  const [filesUploading, setFilesUploading] = useState<boolean>(false);
  const [reportReady, setReportReady] = useState<boolean>(false);
  const [fileReportIds, setFileReportIds] = useState<any[]>([]);
  const [filesDownloading, setFilesDownloading] = useState<boolean>(false);
  const [filesDownloadIds, setFilesDownloadIds] = useState<any[]>([]);
  const [recipients, setRecipients] = useState<any[]>([]);
  const [userRecepientModal, setUserRecepientModal] = useState<boolean>(false);
  const [questionnaireNotificationLoading, setQuestionnaireNotificationLoading] = useState<boolean>(false);
  const [previewIndex, setPreviewIndex] = useState<number | undefined>();
  const [deleteFileId, setDeleteFileId] = useState<string | undefined>(undefined);

  const dispatch = useDispatch();

  const quizGetRestState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "get" });
  const quizUpdateRestState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "update" });
  const quizFilesUploadRestState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "upload" });
  const productGetRestState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "get",
    uuid: quiz?.product_id
  });
  const workItemGetRestState = useParamSelector(getGenericMethodSelector, { uri: "workitem", method: "get" });
  const kbGetRestState = useParamSelector(getGenericMethodSelector, { uri: "bestpractices", method: "get" });
  const filesGetRestState = useParamSelector(getGenericMethodSelector, { uri: "files", method: "get" });
  const sessionRestState = useSelector(mapSessionStatetoProps);
  const quizNotifyRestState = useParamSelector(getGenericMethodSelector, {
    uri: "questionnaires_notify",
    method: "list"
  });

  useEffect(() => {
    if (quizId === undefined) {
      props.history.push(getReportsPage());
    } else {
      dispatch(quizGet(quizId));
      dispatch(genericList("users", "list", {}, null, "mentions_search"));
      setQuizLoading(true);
    }
  }, []);

  useEffect(() => {
    if (quizLoading && hasQuizId) {
      const { loading, error } = get(quizGetRestState, [quizId as string], { loading: true, error: true });
      if (!loading && !error) {
        const data = get(quizGetRestState, [quizId as string, "data"], undefined);
        if (data) {
          const restQuiz = new RestQuiz(data);
          if (restQuiz.product_id !== undefined) {
            dispatch(workspaceRead(restQuiz.product_id, "get"));
            setProductLoading(true);
          }
          if (restQuiz.work_item_id !== undefined) {
            dispatch(workItemGet(restQuiz.work_item_id));
            setWorkItemLoading(true);
          }
          if (isArray(restQuiz.kb_ids) && restQuiz.kb_ids?.length > 0) {
            forEach(restQuiz.kb_ids, (id: any) => {
              dispatch(bpsGet(id));
            });
            setKbsLoading(true);
          }
          setQuiz(restQuiz);
        }
        setQuizLoading(false);
      }
    }
  }, [quizGetRestState]);

  useEffect(() => {
    if (productLoading) {
      const loading = get(productGetRestState, ["loading"], true);
      const error = get(productGetRestState, ["error"], false);
      if (!loading && !error) {
        const data = get(productGetRestState, ["data"], undefined);
        if (data) {
          setProduct(data);
        }
        setProductLoading(false);
      }
    }
  }, [productGetRestState]);

  useEffect(() => {
    if (workItemLoading) {
      const { loading, error } = get(workItemGetRestState, [quiz?.work_item_id], { loading: true, error: false });
      if (!loading && !error) {
        const data = get(workItemGetRestState, [quiz?.work_item_id, "data"], undefined);
        if (data) {
          setWorkItem(data);
        }
        setWorkItemLoading(false);
      }
    }
  }, [workItemGetRestState]);

  useEffect(() => {
    if (kbsLoading) {
      const downloadedIds = map(kbs, kb => kb.id);
      const allIds = quiz?.kb_ids;
      const remainingIds = difference(allIds, downloadedIds);
      if (remainingIds.length > 0) {
        const rKbs: any[] = [];
        forEach(remainingIds, id => {
          const { loading, error } = get(kbGetRestState, [id], { loading: true, error: true });
          if (!loading && !error) {
            const data = get(kbGetRestState, [id, "data"], undefined);
            if (data) {
              rKbs.push(data);
            }
          }
        });
        setKbs((prev: any) => uniqBy([...prev, ...rKbs], "id"));
      } else {
        setKbsLoading(false);
      }
    }
  });

  useEffect(() => {
    if (quizSubmitLoading && hasQuizId) {
      const { loading, error } = get(quizUpdateRestState, [quizId as string], { loading: true, error: true });
      if (!loading && !error) {
        const data = get(quizUpdateRestState, [quizId as string, "data"], undefined);
        if (data) {
          if (deleteFileId) {
            dispatch(filesDelete(deleteFileId));
            setDeleteFileId(undefined);
          }
          setQuiz(new RestQuiz(data));
          setDirtyQuestion({ question: undefined, section: undefined, timestamp: undefined });
          setDirty(false);
          dispatch(restapiClear("quiz", "update", quizId as string));
          setQuizSubmitLoading(false);
        }
      } else if (!loading && error) {
        const errorCode = get(quizUpdateRestState, [quizId as string, "error_code"], 0);
        dispatch(quizGet(quizId));
        setQuizSubmitLoading(false);
        if (errorCode === 409) {
          setResolveConflictLoading(true);
        } else {
          setQuizLoading(true);
          setDirtyQuestion({ question: undefined, section: undefined, timestamp: undefined });
          notification.error({
            message: "Assessment was not updated"
          });
        }
      }
    }
  });

  useEffect(() => {
    if (resolveConflictLoading) {
      const { loading, error } = get(quizGetRestState, [quizId as string], { loading: true, error: true });
      if (!loading && !error) {
        const data = get(quizGetRestState, [quizId as string, "data"], undefined);
        if (data) {
          const restQuiz = new RestQuiz(data);
          const currentAssert = restQuiz.assertion(dirtyQuestion?.section, dirtyQuestion?.question);
          if (currentAssert?.created_at !== dirtyQuestion?.timestamp) {
            notification.error({
              message: "Assessment update conflict",
              description: "Question has been updated by someone else. Please refresh your assessment"
            });
            setResolveConflictLoading(false);
            setQuizSubmitLoading(false);
            setDirtyQuestion({ question: undefined, section: undefined, timestamp: undefined });
          } else {
            if (dirtyQuestion?.question && dirtyQuestion?.section && dirtyQuestion?.timestamp) {
              const dirtyAssert = quiz?.assertion(dirtyQuestion.section, dirtyQuestion.question);
              restQuiz.updateAssertion(dirtyQuestion.section, dirtyQuestion.question, dirtyAssert);
              dispatch(quizUpdate(restQuiz.id, restQuiz));
              setResolveConflictLoading(false);
              setQuizSubmitLoading(true);
            } else {
              notification.error({
                message: "Assessment update conflict",
                description: "Question has been updated by someone else. Please refresh your assessment"
              });
              setResolveConflictLoading(false);
              setQuizSubmitLoading(false);
              setDirtyQuestion({ question: undefined, section: undefined, timestamp: undefined });
            }
          }
        }
      }
    }
  });

  useEffect(() => {
    if (filesUploading) {
      let fileLoading: boolean = false;
      let newQuiz: RestQuiz | undefined = quiz;
      forEach(fileUploads, file => {
        const uploadId = `${newQuiz?.id}:${file.question_id}:${file.index}`;
        const { loading, error } = get(quizFilesUploadRestState, [uploadId], { loading: true, error: true });
        if (!loading && !error) {
          const fileId = get(quizFilesUploadRestState, [uploadId, "data", "id"]);
          for (let i = 0; i < quiz?.section_responses.length; i++) {
            if (file.section_id.toString() === quiz?.section_responses[i].section_id.toString()) {
              for (let j = 0; j < quiz?.section_responses[i].answers.length; j++) {
                if (newQuiz?.section_responses[i].answers[j].question_id.toString() === file.question_id.toString()) {
                  newQuiz!.section_responses[i].answers[j].responses[file.index].value = fileId;
                  newQuiz!.section_responses[i].answers[j].responses[file.index].upload = false;
                  break;
                }
              }
              break;
            }
          }
        } else {
          fileLoading = true;
        }
      });
      if (!fileLoading) {
        dispatch(quizUpdate(newQuiz?.id, newQuiz));
        setQuiz(newQuiz);
        setFilesUploading(false);
        setQuizSubmitLoading(true);
      }
    }
  });

  useEffect(() => {
    if (filesLoading) {
      let fileLoading = false;
      forEach(fileReportIds, file => {
        const { loading, error } = get(filesGetRestState, [file.id], { loading: true, error: true });
        if (loading) {
          fileLoading = true;
        } else {
          const data = get(filesGetRestState, [file.id, "data"], undefined);
          if (!error) {
            if (data === undefined) {
              fileLoading = true;
            }
          }
        }
      });
      setFilesLoading(fileLoading);
      setReportReady(!fileLoading);
    }
  });

  useEffect(() => {
    if (reportReady) {
      let zip = new JSZip();
      let folder = zip.folder("assessments");
      (async () => {
        const quizFromJson = quiz?.json();
        const quizPDF = <DownloadAssessment assessment={quizFromJson} />;
        const fileName = workItem?.vanity_id
          ? `${workItem?.vanity_id}_${quizFromJson?.qtemplate_name}`
          : quizFromJson?.qtemplate_name;
        folder?.file(`${fileName}.pdf`, pdfBlob(quizPDF));
      })();
      forEach(fileReportIds, file => {
        folder?.file(file.name, getFileBlob(get(filesGetRestState, [file.id, "data"], undefined)));
      });
      zip.generateAsync({ type: "blob" }).then(content => FileSaver.saveAs(content, "assessments.zip"));
      setReportReady(false);
      setFileReportIds([]);
    }
  });

  useEffect(() => {
    if (filesDownloading) {
      if (filesDownloadIds.length) {
        const remainingFiles: any[] = [];
        forEach(filesDownloadIds, file => {
          const { loading, error } = get(filesGetRestState, [file.file_id], { loading: true, error: true });
          if (loading || error) {
            remainingFiles.push(file);
          }
        });
        setFilesDownloadIds(remainingFiles);
      } else {
        setFilesDownloading(false);
      }
    }
  });

  useEffect(() => {
    if (questionnaireNotificationLoading) {
      const { loading, error } = get(quizNotifyRestState, ["0"], { loading: true, error: true });
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
        setQuestionnaireNotificationLoading(false);
      }
    }
  });

  const answerQuiz = (event: any, updadtedQuiz: RestQuiz | undefined) => {
    let newQuiz: RestQuiz | undefined = updadtedQuiz;
    newQuiz!.completed = event?.currentTarget?.id === "submit-quiz";
    //TODO: confirm
    // newQuiz!.user_email = ls.getUserEmail();
    const result = getCalculatedRiskScore(newQuiz?.section_responses);
    newQuiz!.current_score = result.score;
    newQuiz!.answered_questions = result.answered;
    let file_uploads: any[] = [];
    forEach(newQuiz?.section_responses, answer => {
      forEach(answer.answers, assertion => {
        if (assertion.answered && assertion.responses !== undefined && assertion.responses[0] !== undefined) {
          forEach(assertion.responses, (response, index) => {
            if (response?.type === "file" && response.upload === true) {
              dispatch(quizFileUpload(`${newQuiz?.id}:${assertion.question_id}:${index}`, response.value));
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
    if (file_uploads.length === 0) {
      dispatch(quizUpdate(newQuiz?.id, newQuiz));
    }
    setQuiz(newQuiz);
    setFileUploads(file_uploads);
    setFilesUploading(file_uploads.length > 0);
    setQuizSubmitLoading(file_uploads.length === 0);
  };

  const sendRecipientsList = useCallback(() => {
    const questionnairedata = {
      questionnaire_id: quiz?.id,
      recipients: [...map(recipients, user => user.label)],
      mode: "SLACK",
      requestor_type: "USER",
      requestor_id: sessionRestState.session_user_id,
      requestor_name: sessionRestState.session_first_name
    };
    dispatch(qsNotify(questionnairedata));
    setUserRecepientModal(false);
    setQuestionnaireNotificationLoading(true);
    setRecipients([]);
  }, [sessionRestState, recipients]);

  const downloadFile = useCallback(
    (e: any, fileId: any, fileName: any) => {
      e?.preventDefault?.();
      if (findIndex(filesDownloadIds, file => file.file_id === fileId) !== -1) {
        return;
      }
      setFilesDownloadIds((prev: any) => [...prev, { file_id: fileId, file_name: fileName }]);
      setFilesDownloading(true);
      dispatch(filesGet(fileId, fileName, true));
    },
    [filesDownloadIds]
  );

  const previewList = useMemo((): any[] => {
    return map(
      filter(kbs, kb => kb.type === "FILE"),
      kb => {
        let fetchFileId = `kb/${kb.id}/${kb.value}`;
        return {
          fileId: kb.value,
          fileName: kb.metadata || "download",
          fetchFileId: fetchFileId
        };
      }
    );
  }, [kbs]);

  const handleUpdate = (action: QuestionsHandlerType, payload: any) => {
    switch (action) {
      case QuestionsHandlerType.SEND_BY_SLACK:
        setUserRecepientModal(true);
        break;
      case QuestionsHandlerType.ANSWER_QUIZ:
        answerQuiz(payload, quiz);
        break;
      case QuestionsHandlerType.PREVIEW_FILE:
        setPreviewIndex(payload);
        break;
      case QuestionsHandlerType.UPDATE_QUIZ:
        setDirty(true);
        const { newQuiz: updatedQuiz, dirty_section, dirty_question, dirty_timestamp, should, deleteFileId } = payload;
        setDirtyQuestion({ question: dirty_question, section: dirty_section, timestamp: dirty_timestamp });

        if (deleteFileId) {
          setDeleteFileId(deleteFileId);
        }

        if (should) {
          answerQuiz(undefined, updatedQuiz);
        }
        break;
      case QuestionsHandlerType.DOWNLOAD_FILE:
        const { event, fileId, fileName } = payload;
        downloadFile(event, fileId, fileName);
        break;
      default:
        break;
    }
  };

  if (quizLoading) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
        <Loader />
      </div>
    );
  }

  return (
    <>
      {previewIndex !== undefined && (
        <PreviewerComponent
          onClose={() => setPreviewIndex(undefined)}
          onDownload={downloadFile}
          list={previewList}
          currentIndex={previewIndex}
        />
      )}
      {userRecepientModal && (
        <RecepientsModal
          visible={userRecepientModal}
          onCancel={() => {
            setUserRecepientModal(false);
            setRecipients([]);
          }}
          onOk={sendRecipientsList}
          selectedRecipients={recipients}
          onUpdateRecipients={(newRecipients: any) => setRecipients(newRecipients)}
        />
      )}
      <AnswerQuestionnaireComponent
        quiz={quiz}
        workItem={workItem}
        product={product}
        kbFiles={kbs}
        fileDownloadIds={filesDownloadIds}
        disabled={quizSubmitLoading || resolveConflictLoading ? dirtyQuestion?.question : false}
        onUpdate={handleUpdate}
        workItemLoading={workItemLoading}
        productLoading={productLoading}
      />
    </>
  );
};

export default ConfirmationWrapper(AnswerQuestionnaireContainer);
