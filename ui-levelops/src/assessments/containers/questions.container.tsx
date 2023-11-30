import { Card, Col, Row } from "antd";
import QuestionsComponent from "assessments/components/questions/QuestionsComponent";
import {
  extractResponse,
  filterResponses,
  getAssertion,
  getPrevCreatedAt,
  getResponse,
  mapAllFiles,
  modifyQuiz,
  QuestionsHandlerType
} from "assessments/utils/helper";
import { RestQuiz } from "classes/RestQuiz";
import { filter, find, findIndex, forEach, get, isEqual, map } from "lodash";
import moment from "moment";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import LocalStoreService from "services/localStoreService";
import { AntText, AntTitle } from "shared-resources/components";

interface QuestionsContainerProps {
  quiz: RestQuiz | undefined;
  disabled: boolean;
  expandComments: boolean;
  fileDownloadingIds: Array<any>;
  handleUpdateQuiz: (
    updatedQuiz: any,
    sectionId: string,
    questionId: string,
    should?: boolean,
    prevTimeStamp?: any,
    deleteFileId?: string
  ) => void;
  handleDownloadFile: (event: any, fileId: string, fileName: string) => void;
  handlePreviewFile: (fileId: string) => void;
}

const QuestionsContainer: React.FC<QuestionsContainerProps> = props => {
  const { quiz, handleUpdateQuiz, handleDownloadFile, disabled, expandComments } = props;

  const ls = new LocalStoreService();

  const userEmail = ls.getUserEmail();

  const [showComments, setShowComments] = useState<any>(undefined);
  const [linkUpload, setLinkUpload] = useState<any>({});
  const [showPreviewer, setShowPreviewer] = useState<boolean>(false);
  const [previewList, setPreviewList] = useState<any>([]);
  const [currentPreviewIndex, setCurrentPreviewIndex] = useState<number>(-1);
  const [allFiles, setAllFiles] = useState<any>({});

  const sectionResponseRef = useRef<any[]>([]);

  useEffect(() => {
    if (!isEqual(sectionResponseRef.current, quiz?.section_responses)) {
      sectionResponseRef.current = quiz?.section_responses || [];
      setAllFiles(mapAllFiles(quiz));
    }
  }, [quiz]);

  const closePreviewer = useCallback(() => {
    setShowPreviewer(false);
    setPreviewList([]);
    setCurrentPreviewIndex(-1);
  }, []);

  const previewFile = (payload: any) => {
    const { sectionId, questionId, fileId } = payload;
    let assertion = getAssertion(quiz?.section_responses, sectionId, questionId);
    let currentIndex = -1;
    if (!assertion) {
      return;
    }

    const fileIds = filterResponses(assertion.responses, "nolink").map((response: any) => {
      const { fileId, fileName, uploadNeeded } = extractResponse(response);
      if (assertion.answered === false || uploadNeeded === true) {
        return "";
      }
      let fetchFileId = `quiz/${quiz?.id}/assertion/${questionId}/${fileId}`;
      return {
        fileId: fileId,
        fileName: fileName,
        fetchFileId: fetchFileId
      };
    });

    currentIndex = findIndex(fileIds, (f: any) => f.fileId === fileId);
    if (currentIndex !== -1) {
      setShowPreviewer(true);
      setPreviewList(fileIds);
      setCurrentPreviewIndex(currentIndex);
    }
  };

  const updateMultiSelectAndCheckList = (payload: any, type: string = "checklist") => {
    const { sectionId, questionId, value } = payload;
    let curQuiz = quiz;
    const updatedValues = value.map((val: any) => JSON.parse(val));
    let answer = filter(curQuiz?.section_responses, (answer: any) => answer.section_id.toString() === sectionId)[0];
    const { prevCreatedAt, newAnswer } = getPrevCreatedAt(answer, questionId, updatedValues, true, userEmail);
    answer = newAnswer;
    if (type === "multiselect") {
      curQuiz = modifyQuiz(curQuiz, sectionId, answer);
    }
    handleUpdateQuiz(curQuiz, sectionId, questionId, true, prevCreatedAt);
  };

  const updateSingleSelect = (payload: any) => {
    const { event } = payload;
    const { value, score, question, section, originalValue } = event.target;
    let curQuiz = quiz;
    let answer = filter(
      curQuiz?.section_responses,
      (answer: any) => answer.section_id.toString() === section.toString()
    )[0];
    const updatedValues = [{ value: value, score: score, original_value: originalValue }];
    const { prevCreatedAt, newAnswer } = getPrevCreatedAt(answer, question, updatedValues, true, userEmail);
    answer = newAnswer;
    curQuiz = modifyQuiz(curQuiz, question, answer);
    handleUpdateQuiz(curQuiz, section, question, true, prevCreatedAt);
  };

  const updateTextValue = (payload: any) => {
    const { questionValue, sectionId, questionId, score, should } = payload;
    const value = questionValue;
    let curQuiz = quiz;
    let answer = filter(curQuiz?.section_responses, (answer: any) => answer.section_id.toString() === sectionId)[0];
    const updatedValues = [{ value: value, score: score }];
    const { prevCreatedAt, newAnswer } = getPrevCreatedAt(answer, questionId, updatedValues, value !== "", userEmail);
    answer = newAnswer;
    curQuiz = modifyQuiz(curQuiz, sectionId, answer);
    handleUpdateQuiz(curQuiz, sectionId, questionId, should, prevCreatedAt);
  };

  const handleFileUpload = (payload: any) => {
    let { file, sectionId, questionId, score, link } = payload;
    if (!link) {
      link = false;
    }
    let curQuiz = quiz;
    let answers = curQuiz?.section_responses;
    let prevCreatedAt = undefined;
    const allFiles: any[] = [];
    for (let i = 0; i < answers.length; i++) {
      if (answers[i].section_id.toString() === sectionId) {
        for (let j = 0; j < answers[i].answers.length; j++) {
          if (answers[i].answers[j].question_id.toString() === questionId) {
            prevCreatedAt = answers[i].answers[j].created_at;
            answers[i].answers[j].user_email = file !== undefined ? userEmail : undefined;
            answers[i].answers[j].created_at = file !== undefined ? moment().unix() : undefined;
            answers[i].answers[j].answered = file !== undefined;
            answers[i].answers[j].upload = file !== undefined && link === false ? true : undefined;
            if (link === true) {
              answers[i].answers[j].upload = false;
            }
            const responseLength = answers[i].answers[j].responses.length;
            const response = {
              ...getResponse(file, responseLength, score, link),
              user: file !== undefined ? userEmail : undefined,
              created_at: file !== undefined ? moment().unix() : undefined
            };
            answers[i].answers[j].responses.push(response);
            allFiles.push(response);
          }
        }
      }
    }
    curQuiz!.section_responses = answers;
    setAllFiles(mapAllFiles(curQuiz));
    handleUpdateQuiz(curQuiz, sectionId, questionId, true, prevCreatedAt);
  };

  const handleFileRemove = (payload: any) => {
    const { file, sectionId, questionId } = payload;
    let curQuiz = quiz;
    let prevCreatedAt = undefined;
    let answer = filter(curQuiz?.section_responses, (answer: any) => answer.section_id.toString() === sectionId)[0];
    answer.answers.forEach((assert: any) => {
      if (assert.question_id === questionId) {
        prevCreatedAt = assert.created_at;
        const newResponses = assert.responses.filter(
          (response: any) => get(response, ["value", "uid"], "0") !== file.uid
        );
        assert.created_at = moment().unix();
        assert.responses = newResponses;
      }
    });
    setAllFiles(mapAllFiles(curQuiz));
    handleUpdateQuiz(curQuiz, sectionId, questionId, true, prevCreatedAt);
  };

  const removeFile = (payload: any) => {
    const { sectionId, questionId, id, isLink } = payload;
    let curQuiz = quiz;
    let prevCreatedAt = undefined;
    let answer = filter(curQuiz?.section_responses, (answer: any) => answer.section_id.toString() === sectionId)[0];
    let fileId = undefined;
    (answer.answers || []).forEach((assert: any) => {
      if (assert.question_id === questionId) {
        const index = findIndex(assert.responses || [], (r: any) => r.value === id);
        if (!isLink && assert.responses[index].type === "file") {
          fileId = `quiz/${quiz?.id}/assertion/${questionId}/${assert.responses[index].value}`;
        }
        prevCreatedAt = assert.created_at;
        assert.created_at = moment().unix();

        assert.responses.splice(index, 1);
        if (assert.responses.length === 0) {
          assert.answered = false;
        }
      }
    });
    setAllFiles(mapAllFiles(curQuiz));
    handleUpdateQuiz(curQuiz, sectionId, questionId, true, prevCreatedAt, fileId);
  };

  const updateOptionValue = (payload: any) => {
    const { sectionId, questionId, index, e } = payload;
    let section = find(quiz?.sections, (section: any) => section.id === sectionId);
    let question = section.questions.filter((question: any) => question.id === questionId)[0];
    question.options[index].updatedValue = e.currentTarget.value;
    handleUpdateQuiz(quiz, sectionId, questionId, false, undefined);
  };

  const updateComments = (payload: any) => {
    const { sectionId, questionId, comment } = payload;
    let prevCreatedAt: any = undefined;
    let answer = filter(quiz?.section_responses, (answer: any) => answer.section_id.toString() === sectionId)[0];
    answer.answers.forEach((assert: any) => {
      if (assert.question_id === questionId) {
        let comments = assert.comments || [];
        comments.push(comment);
        prevCreatedAt = assert.created_at;
        assert.comments = comments;
        assert.created_at = moment().unix();
      }
    });
    forEach(quiz?.section_responses, (ans: any) => {
      if (ans.section_id.toString() === sectionId) {
        ans.answers = answer.answers;
      }
    });
    handleUpdateQuiz(quiz, sectionId, questionId, true, prevCreatedAt);
  };

  const handleShowComments = (payload: any) => {
    if (payload) {
      const { sectionId, questionId } = payload;
      setShowComments({ section_id: sectionId, question_id: questionId });
    } else {
      setShowComments(undefined);
    }
  };

  const getSection = useMemo(
    () => (sectionId: any) => {
      return filter(quiz?.sections, (section: any) => section.id === sectionId)[0];
    },
    [quiz]
  );

  const questionsHandlers = useCallback(
    (type: QuestionsHandlerType, payload?: any) => {
      switch (type) {
        case QuestionsHandlerType.CLOSE_PREVIEWER:
          closePreviewer();
          break;
        case QuestionsHandlerType.PREVIEW_FILE:
          previewFile(payload);
          break;
        case QuestionsHandlerType.GET_ASSERTION:
          return getAssertion(quiz?.section_responses, payload?.sectionId, payload?.questionId);
        case QuestionsHandlerType.FILE_UPLOAD:
          handleFileUpload(payload);
          break;
        case QuestionsHandlerType.FILE_REMOVE:
          handleFileRemove(payload);
          break;
        case QuestionsHandlerType.REMOVE_FILE:
          removeFile(payload);
          break;
        case QuestionsHandlerType.SHOW_COMMENTS:
          handleShowComments(payload);
          break;
        case QuestionsHandlerType.UPLOAD_LINK:
          const { value } = payload;
          setLinkUpload(value);
          break;
        case QuestionsHandlerType.UPDATE_CHECKLIST:
          updateMultiSelectAndCheckList(payload);
          break;
        case QuestionsHandlerType.UPDATE_COMMENTS:
          updateComments(payload);
          break;
        case QuestionsHandlerType.UPDATE_MULTI_SELECT:
          updateMultiSelectAndCheckList(payload, "multiselect");
          break;
        case QuestionsHandlerType.UPDATE_OPTION:
          updateOptionValue(payload);
          break;
        case QuestionsHandlerType.UPDATE_SINGLE_SELECT:
          updateSingleSelect(payload);
          break;
        case QuestionsHandlerType.UPDATE_TEXT_VALUE:
          updateTextValue(payload);
          break;
        case QuestionsHandlerType.DOWNLOAD_FILE:
          const { event, fileId, fileName } = payload;
          handleDownloadFile(event, fileId, fileName);
          break;
      }
    },
    [quiz]
  );

  return (
    <>
      {map(quiz?.sections || [], section => (
        <Col span={24} key={section.id}>
          <Card>
            <Row type={"flex"} justify={"space-between"} gutter={[20, 20]}>
              <Col span={6}>
                <div>
                  <AntTitle level={4}>{section?.name || ""}</AntTitle>
                </div>
                <div>
                  <AntText type={"secondary"} className="word-break-all">
                    {section?.description || ""}
                  </AntText>
                </div>
              </Col>
              <Col span={17}>
                <QuestionsComponent
                  questionHandlers={questionsHandlers}
                  showPreviewer={showPreviewer}
                  previewList={previewList}
                  previewIndex={currentPreviewIndex}
                  section={getSection(section?.id)}
                  showComments={showComments}
                  disabled={disabled}
                  linkUpload={linkUpload}
                  expandComments={expandComments}
                  allFiles={allFiles?.[section?.id] || {}}
                />
              </Col>
            </Row>
          </Card>
        </Col>
      ))}
    </>
  );
};

export default QuestionsContainer;
