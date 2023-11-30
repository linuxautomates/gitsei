import "./questions.style.scss";

import { Divider, Form, Icon, Input, Upload } from "antd";
import {
  assertionSort,
  checkShowComments,
  extractAnswer,
  filterResponses,
  getCheckListValues,
  getMultiselectValues,
  multiselectValue,
  QuestionsHandlerType,
  singleSelectOriginalValue,
  singleSelectValue
} from "assessments/utils/helper";
import { cloneDeep, forEach, get, map } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import LocalStoreService from "services/localStoreService";
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
import { AttachmentItem } from "shared-resources/components/attachment-item/attachment-item.component";
import PreviewerComponent from "shared-resources/components/previewer/previewer";

import { Comments } from "..";
import { getGenericMethodSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";

// @ts-ignore
import sanitizeHtml from "sanitize-html";
import { validateURL } from "utils/stringUtils";

interface QuestionsComponentProps {
  questionHandlers: (type: QuestionsHandlerType, payload?: any) => any;
  showPreviewer: boolean;
  previewList: any;
  previewIndex: number;
  section: any;
  showComments: any;
  disabled: boolean;
  linkUpload: any;
  expandComments: boolean;
  allFiles: any;
}

const QuestionsComponent: React.FC<QuestionsComponentProps> = props => {
  const {
    questionHandlers,
    showPreviewer,
    previewIndex,
    previewList,
    section,
    showComments,
    disabled,
    linkUpload,
    expandComments,
    allFiles
  } = props;
  const [questionValues, setQuestionValues] = useState<any>({});
  const [isInvalidLink, setIsInvalidLink] = useState<boolean>(false);

  const ls = new LocalStoreService();

  const quizUploadState = useParamSelector(getGenericMethodSelector, { uri: "quiz", method: "upload" });

  const mapResponseWithRestState = useCallback(
    (response: any) => {
      const uploadId = response.upload_id;
      if (uploadId) {
        const data = get(quizUploadState, [uploadId], undefined);
        if (data !== undefined) {
          return { ...response, uploading: data.loading || false };
        }
        return response;
      }
      return response;
    },
    [quizUploadState]
  );

  useEffect(() => {
    setQuestionValues(initialQuestionValues());
  }, []);

  const initialQuestionValues = useCallback(() => {
    let curvalues: any = {};
    forEach(section.questions, (question: any, index: number) => {
      const answer = questionHandlers(QuestionsHandlerType.GET_ASSERTION, {
        sectionId: section.id,
        questionId: question.id
      });
      const textValue = answer ? get(answer, ["responses", 0, "value"], "") : "";
      curvalues[question.id] = textValue;
      const linkId = `${section.id}${question.id}`;
      curvalues[linkId] = get(linkUpload, linkId, "");
    });
    return curvalues;
  }, []);

  const buildFileUploadLink = useMemo(
    () => (sectionId: any, questionId: any) => {
      let assertion = questionHandlers(QuestionsHandlerType.GET_ASSERTION, { sectionId, questionId });
      if (!assertion) {
        return "";
      }
      const links = map(filterResponses(allFiles?.[questionId] || [], "link"), (response: any, index: number) => {
        const { value: fileId, upload: uploadNeeded } = response;

        if (assertion.answered === false || uploadNeeded === true) {
          return null;
        }
        return (
          <AttachmentItem
            fileName={fileId}
            trigger="click"
            type="link"
            fileResponse={response}
            removeFile={() =>
              questionHandlers(QuestionsHandlerType.REMOVE_FILE, {
                sectionId,
                questionId,
                id: fileId,
                isLink: true
              })
            }
          />
        );
      });

      const nonLinks = map(filterResponses(allFiles?.[questionId] || [], "nolink"), (response: any, index: number) => {
        const {
          value: fileId,
          file_name: fileName,
          upload: uploadNeeded,
          uploading
        } = mapResponseWithRestState(response);

        if (assertion.answered === false) {
          return null;
        }
        return (
          <AttachmentItem
            loading={uploading}
            fileName={fileName}
            fileResponse={response}
            trigger="click"
            previewFile={() => questionHandlers(QuestionsHandlerType.PREVIEW_FILE, { sectionId, questionId, fileId })}
            removeFile={() =>
              questionHandlers(QuestionsHandlerType.REMOVE_FILE, {
                sectionId,
                questionId,
                id: fileId,
                isLink: false
              })
            }
          />
        );
      });
      return (
        <>
          {nonLinks}
          {links}
        </>
      );
    },
    [section, allFiles, quizUploadState]
  );

  const handleUpdateTextValue = (
    questionValue: string,
    sectionId: string,
    questionId: string,
    should: boolean,
    score: any
  ) => {
    questionHandlers(QuestionsHandlerType.UPDATE_TEXT_VALUE, {
      questionValue,
      sectionId,
      questionId,
      score,
      should
    });
  };

  const handleValueChange = (id: string, value: string) => {
    const newQuestionValues = cloneDeep(questionValues);
    Object.keys(questionValues).map((key: string) => {
      if (key === id) {
        newQuestionValues[id] = value;
      }
    });
    setQuestionValues(newQuestionValues);
  };

  const handleLinkUpload = () => {
    let curLinkUpload: any = {};
    forEach(section.questions, (question: any, index: number) => {
      const id = `${section.id}${question.id}`;
      curLinkUpload[id] = get(questionValues, id, "");
    });
    return curLinkUpload;
  };

  const handleOnPressEnter = (e: any, question: any) => {
    e?.preventDefault();
    const value = e?.currentTarget?.value;
    let curLinkUpload = handleLinkUpload();
    curLinkUpload[`${section?.id}${question?.id}`] = "";
    if (validateURL(value)) {
      questionHandlers(QuestionsHandlerType.UPLOAD_LINK, { value: { ...curLinkUpload } });
      questionHandlers(QuestionsHandlerType.FILE_UPLOAD, {
        file: value,
        sectionId: section?.id,
        questionId: question?.id,
        score: get(question, ["options"], []).length ? question?.options[0]?.score : "",
        link: true
      });
    } else {
      setIsInvalidLink(true);
    }
  };

  const handleLinkChange = (questionId: string, value: string) => {
    if (isInvalidLink) {
      setIsInvalidLink(false);
    }
    handleValueChange(questionId, value);
  };

  const getValidStatus = useMemo(() => {
    return isInvalidLink ? "error" : "validating";
  }, [isInvalidLink]);

  const getHelpText = useMemo(() => {
    return isInvalidLink ? "Invalid URL Format" : "";
  }, [isInvalidLink]);

  const buildAssertions = () => {
    let result: JSX.Element[] = [];
    forEach(assertionSort(section.questions), (question: any, index: number) => {
      let assertionField: any = null;
      const answer = questionHandlers(QuestionsHandlerType.GET_ASSERTION, {
        sectionId: section.id,
        questionId: question.id
      });
      const { userEmail, createdOn, comments } = extractAnswer(answer);

      switch (question.type.toLowerCase()) {
        case "checklist":
          const checklistValues = getCheckListValues(answer);
          assertionField = (
            <AntCheckboxGroup
              style={{ width: "100%" }}
              disabled={disabled !== false && disabled !== question.id}
              value={checklistValues}
              onChange={(value: any) => {
                questionHandlers(QuestionsHandlerType.UPDATE_CHECKLIST, {
                  sectionId: section.id,
                  questionId: question.id,
                  value
                });
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
              disabled={disabled !== false && disabled !== question.id}
              onChange={(e: any) => {
                questionHandlers(QuestionsHandlerType.UPDATE_SINGLE_SELECT, { event: e });
              }}>
              {map(question.options || [], (option: any, oi: number) => {
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
          const textValue = get(questionValues, [question.id], "");
          assertionField = (
            <AntInput
              disabled={disabled !== false && disabled !== question.id}
              type="textarea"
              name={`assertion:text:${question.id}:${question.options[0].score}`}
              value={textValue}
              onBlur={(e: any) => {
                const questionValue = get(questionValues, question.id, "");
                handleUpdateTextValue(questionValue, section.id, question.id, true, question.options[0].score);
              }}
              onChange={(e: any) => handleValueChange(question.id, e.target.value)}
              autoSize={{ minRows: 3, maxRows: 5 }}
            />
          );
          break;
        case "multi-select":
          const multiValues = getMultiselectValues(answer);
          assertionField = (
            <AntCheckboxGroup
              disabled={disabled !== false && disabled !== question.id}
              style={{ width: "100%" }}
              value={multiValues}
              onChange={(value: any) => {
                questionHandlers(QuestionsHandlerType.UPDATE_MULTI_SELECT, {
                  sectionId: section.id,
                  questionId: question.id,
                  value
                });
              }}>
              <AntRow gutter={[20, 20]}>
                {map(question.options || [], (option: any, oi: number) => (
                  <AntCol span={24}>
                    <AntCheckbox
                      key={oi}
                      value={JSON.stringify({
                        value: multiselectValue(option, answer),
                        score: option.score,
                        original_value: option.value
                      })}>
                      {option.editable && (
                        <AntInput
                          value={multiselectValue(option, answer)}
                          style={{ width: 250 }}
                          onChange={(e: any) =>
                            questionHandlers(QuestionsHandlerType.UPDATE_OPTION, {
                              sectionId: section.id,
                              questionId: question.id,
                              index: oi,
                              e
                            })
                          }
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
          const originalValue = singleSelectOriginalValue(singleValue, answer);
          assertionField = (
            <AntRadioGroup
              value={singleValue}
              onChange={(e: any) => questionHandlers(QuestionsHandlerType.UPDATE_SINGLE_SELECT, { event: e })}
              disabled={disabled !== false && disabled !== question.id}>
              <AntRow gutter={[20, 20]}>
                {map(question.options || [], (option: any, oi: number) => (
                  <AntCol span={24}>
                    <AntRadio
                      key={oi}
                      value={singleSelectValue(option, singleValue, originalValue)}
                      originalValue={option.value}
                      score={option.score}
                      question={question.id}
                      section={section.id}>
                      {option.editable && (
                        <AntInput
                          disabled={disabled !== false && disabled !== question.id}
                          value={singleSelectValue(option, singleValue, originalValue)}
                          style={{ width: 250 }}
                          onChange={(e: any) =>
                            questionHandlers(QuestionsHandlerType.UPDATE_OPTION, {
                              sectionId: section.id,
                              questionId: question.id,
                              index: oi,
                              e
                            })
                          }
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
          const linkId = `${section.id}${question.id}`;
          const linkValue = get(questionValues, linkId, "");
          assertionField = (
            <AntRow gutter={[10, 10]}>
              <AntCol>
                <AntRow gutter={[0, 0]} type={"flex"} justify={"start"} align={"middle"}>
                  <AntCol span={6}>
                    <>
                      <Upload
                        disabled={disabled !== false && disabled !== question.id}
                        multiple={true}
                        beforeUpload={file => {
                          questionHandlers(QuestionsHandlerType.FILE_UPLOAD, {
                            file,
                            sectionId: section.id,
                            questionId: question.id,
                            score: question.options[0].score
                          });
                          return false;
                        }}
                        onRemove={file => {
                          questionHandlers(QuestionsHandlerType.FILE_REMOVE, {
                            file,
                            sectionId: section.id,
                            questionId: question.id
                          });
                        }}
                        showUploadList={false}>
                        <AntButton>
                          <Icon type="file" />
                          Upload File
                        </AntButton>
                      </Upload>
                    </>
                  </AntCol>
                  <AntCol span={18}>
                    <Form.Item
                      className="question-link-input"
                      colon={false}
                      key="question-link-input"
                      validateStatus={getValidStatus}
                      help={getHelpText}>
                      <Input
                        disabled={disabled !== false && disabled !== question.id}
                        addonBefore={<Icon type="link" />}
                        placeholder={"Add a link here"}
                        allowClear={true}
                        value={linkValue}
                        onChange={e => {
                          handleLinkChange(`${section.id}${question.id}`, sanitizeHtml(e.currentTarget.value));
                        }}
                        onPressEnter={e => handleOnPressEnter(e, question)}
                      />
                    </Form.Item>
                  </AntCol>
                </AntRow>
              </AntCol>
              <AntCol>{buildFileUploadLink(section.id, question.id)}</AntCol>
            </AntRow>
          );
          break;
      }

      const curShowComments = checkShowComments(showComments, section.id, question.id, expandComments);

      result.push(
        <AntRow type={"flex"} justify={"space-between"} gutter={[20, 20]} key={index}>
          <AntCol span={24}>
            {question.type !== "checklist" && (
              <div style={{ margin: "10px" }}>
                <AntText style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>{question.name}</AntText>
              </div>
            )}

            <div style={{ margin: "10px" }}>{assertionField}</div>

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
                    curShowComments
                      ? questionHandlers(QuestionsHandlerType.SHOW_COMMENTS, undefined)
                      : questionHandlers(QuestionsHandlerType.SHOW_COMMENTS, {
                          sectionId: section.id,
                          questionId: question.id
                        })
                  }>
                  <SvgIcon
                    icon={"comments"}
                    theme={comments.length > 0 ? "filled" : "outlined"}
                    className={comments.length === 0 ? "comment-icon" : "comment-icon__enabled"}
                  />
                  {comments.length > 0 && !curShowComments && <AntText>{comments.length} Comments</AntText>}
                  {curShowComments && <AntText>Hide Comments</AntText>}
                  {comments.length === 0 && !curShowComments && <AntText>Add Comment</AntText>}
                </div>
              </AntCol>
              <AntCol span={20}>
                {answer.answered && (
                  <div style={{ paddingBottom: "7px" }}>
                    <AntText type={"secondary"}>ANSWERED BY</AntText>
                    <AntText>
                      {" "}
                      {userEmail} on {createdOn}
                    </AntText>
                  </div>
                )}
              </AntCol>
              {curShowComments && (
                <div className={"comment-wrapper"}>
                  <Comments
                    comments={comments}
                    creator={ls.getUserEmail()}
                    onAddComment={(comment: any) =>
                      questionHandlers(QuestionsHandlerType.UPDATE_COMMENTS, {
                        sectionId: section.id,
                        questionId: question.id,
                        comment
                      })
                    }
                  />
                </div>
              )}
            </AntRow>
          </AntCol>
          {index !== section.questions.length - 1 && question.type !== "checklist" && <Divider />}
        </AntRow>
      );
    });
    return result;
  };

  return (
    <>
      {buildAssertions()}
      {showPreviewer && (
        <PreviewerComponent
          onClose={(e: any) => questionHandlers(QuestionsHandlerType.CLOSE_PREVIEWER)}
          onDownload={(event: any, fileId: string, fileName: string) =>
            questionHandlers(QuestionsHandlerType.DOWNLOAD_FILE, { event, fileId, fileName })
          }
          list={previewList}
          currentIndex={previewIndex}
        />
      )}
    </>
  );
};

export default React.memo(QuestionsComponent);
