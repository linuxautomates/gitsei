import { Card, Col, Row, Button, Checkbox, Divider, Progress, Icon } from "antd";
import { Link } from "react-router-dom";
import QuestionsContainer from "assessments/containers/questions.container";
import { QuestionsHandlerType } from "assessments/utils/helper";
import { RestQuiz } from "classes/RestQuiz";
import Loader from "components/Loader/Loader";
import { getWorkitemDetailPage } from "constants/routePaths";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";
import { filter, get, isNull, isUndefined, map } from "lodash";
import React, { useCallback, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import LocalStoreService from "services/localStoreService";
import { SvgIcon, AntText, AntTitle, AvatarWithText, NameAvatarList, NameAvatar } from "shared-resources/components";
import { AttachmentItem } from "shared-resources/components/attachment-item/attachment-item.component";
import { GenericMentions } from "shared-resources/containers";
import { buildLink } from "utils/integrationUtils";

interface AnswerQuestionnaireComponentProps {
  quiz: RestQuiz | undefined;
  workItem: any;
  product?: WorkspaceModel;
  kbFiles: any[];
  fileDownloadIds: any[];
  disabled: boolean;
  workItemLoading: boolean;
  productLoading: boolean;
  onUpdate: (action: QuestionsHandlerType, payload: any) => void;
}

const AnswerQuestionnaireComponent: React.FC<AnswerQuestionnaireComponentProps> = (
  props: AnswerQuestionnaireComponentProps
) => {
  const ls = new LocalStoreService();
  const className = "answer-questionnaire";

  const dispatch = useDispatch();

  const [includeArtifacts, setIncludeArtifacts] = useState<boolean>(true);
  const [showAllComments, setShowAllComments] = useState<boolean>(true);

  const { quiz, onUpdate, workItem, product, kbFiles, workItemLoading, productLoading } = props;

  const [comments, setComments] = useState<String>(quiz?.comments || "");
  const progressValue = Math.ceil(((quiz?.answered_questions || 0) / (quiz?.total_questions || 0)) * 100);

  const buildArtifactLink = useCallback(() => {
    if (
      isNull(quiz?.integration_url) ||
      isNull(quiz?.integration_application) ||
      isUndefined(quiz?.integration_url) ||
      isUndefined(quiz?.integration_application)
    ) {
      return null;
    }
    return (
      <a
        target="_blank"
        rel="noopener noreferrer"
        className={`${className}__artifact_link`}
        href={buildLink(quiz?.artifact, quiz?.integration_url, quiz?.integration_application)}>
        {quiz?.artifact || ""}
      </a>
    );
  }, [quiz]);

  const handlePDF = useCallback(() => {
    const filesToDownload = [
      {
        id: quiz?.id,
        include_artifacts: includeArtifacts
      }
    ];
    dispatch(genericList("assessment_download", "list", filesToDownload, null, "0"));
  }, [quiz, includeArtifacts]);

  const loader = useMemo(() => <Loader />, []);

  const renderAssignees = useMemo(() => {
    if (workItemLoading) {
      return loader;
    }
    const assignees = get(workItem, ["assignees"], []);
    if (assignees.length === 1) {
      return map(assignees, user => <AvatarWithText text={user.user_email} />);
    } else if (assignees.length > 1) {
      return (
        <NameAvatarList
          className="avatar-list"
          names={map(assignees, assignee => assignee.user_email)}
          classRequired={false}
        />
      );
    } else {
      return "UNASSIGNED";
    }
  }, [workItem, workItemLoading]);

  const renderVanityId = useMemo(() => {
    if (workItemLoading) {
      return loader;
    }
    if (!workItem.vanity_id) {
      return null;
    }
    return (
      <Link
        target="_blank"
        rel="noopener noreferrer"
        to={`${getWorkitemDetailPage()}?workitem=${workItem.vanity_id}`}
        className={`${className}__artifact_link`}>
        {workItem.vanity_id}
      </Link>
    );
  }, [workItem, workItemLoading]);

  return (
    <Row gutter={[20, 20]}>
      <Col span={24}>
        <Card>
          <div>
            <SvgIcon icon={"levelops"} style={{ height: "4rem", width: "4rem" }} />
            <AntText type={"secondary"}>Powered by LevelOps</AntText>
          </div>
          <Row type={"flex"} justify={"space-between"} align={"bottom"}>
            <Col>
              <AntTitle level={4} copyable={{ text: `${window.location.href}&tenant=${ls.getUserCompany()}` }}>
                {quiz?.qtemplate_name}
              </AntTitle>
            </Col>
            <Col>
              <div>
                <Button
                  id={"submit-quiz"}
                  onClick={() => onUpdate(QuestionsHandlerType.SEND_BY_SLACK, undefined)}
                  style={{ marginRight: "0.5rem" }}>
                  Send by Slack
                </Button>
                <Button
                  type={"primary"}
                  id={"submit-quiz"}
                  onClick={e => onUpdate(QuestionsHandlerType.ANSWER_QUIZ, e)}>
                  {quiz?.completed ? "Re-Submit" : "Submit For Review"}
                </Button>
              </div>
            </Col>
          </Row>
          <Row>
            <Col>
              <Checkbox checked={includeArtifacts} onChange={e => setIncludeArtifacts(e.target.checked)}>
                Include Artifacts
              </Checkbox>
              <Button icon={"download"} onClick={handlePDF}>
                Download Assessment
              </Button>
            </Col>
          </Row>
          <Divider />
          <Row type={"flex"} justify={"space-between"} align={"top"}>
            <Col span={2}>
              <Progress strokeWidth={10} percent={progressValue} type={"circle"} width={50} />
            </Col>
            <Col span={4}>
              <div className={`${className}__label`}>Artifact</div>
              <div className={`${className}__artifact_container`}>
                {buildArtifactLink()}
                {renderVanityId}
              </div>
            </Col>
            <Col span={4}>
              <div className={`${className}__label`}>Sent To</div>
              <div className={`${className}__info-value`}>{renderAssignees}</div>
            </Col>
            <Col span={5}>
              <div className={`${className}__label`}>Sent By</div>
              <div className={`${className}__info-value`}>
                <AvatarWithText text={quiz?.sender_email} />
              </div>
            </Col>
            {quiz?.product_id && (
              <Col span={4}>
                <div className={`${className}__label`}>{WORKSPACE_NAME_MAPPING[WORKSPACES]}</div>
                <div className={`${className}__info-value`}>{productLoading ? loader : product?.name || "UNKNOWN"}</div>
              </Col>
            )}
            <Col span={4}>
              <Button onClick={() => setShowAllComments((prev: boolean) => !prev)}>
                {showAllComments ? "Collapse All Comments" : "Expand All Comments"}
              </Button>
            </Col>
          </Row>
          {quiz?.kb_ids?.length > 0 && (
            <>
              <Divider />
              <Row type={"flex"} justify={"start"} align={"top"}>
                <Col span={24}>
                  <div className={`${className}__label`}>Reference Documents</div>
                  <div className={`${className}__info-value`}>
                    {map(
                      filter(kbFiles, kb => kb.type === "LINK"),
                      link => (
                        <Row gutter={[10, 10]}>
                          <Col span={1}>
                            <Icon type={"link"} style={{ fontSize: "14px", marginLeft: "5px" }} />
                          </Col>
                          <Col span={23}>
                            <a href={link.value} target={"_blank"}>
                              {link.name}
                            </a>
                          </Col>
                        </Row>
                      )
                    )}
                    {map(
                      filter(kbFiles, kb => kb.type === "FILE"),
                      (file, index) => (
                        <AttachmentItem
                          fileName={file.metadata || "download"}
                          previewFile={() => onUpdate(QuestionsHandlerType.PREVIEW_FILE, index)}
                        />
                      )
                    )}
                  </div>
                </Col>
              </Row>
            </>
          )}
        </Card>
      </Col>
      <QuestionsContainer
        quiz={quiz}
        disabled={props.disabled}
        expandComments={showAllComments}
        fileDownloadingIds={props.fileDownloadIds}
        handleUpdateQuiz={(
          updatedQuiz: any,
          sectionId: string,
          questionId: string,
          should?: boolean,
          prevTimeStamp?: any,
          deleteFileId?: string
        ) => {
          onUpdate(QuestionsHandlerType.UPDATE_QUIZ, {
            newQuiz: updatedQuiz,
            sectionId,
            questionId,
            should,
            prevTimeStamp,
            deleteFileId
          });
        }}
        handleDownloadFile={(event: any, fileId: any, fileName: any) =>
          onUpdate(QuestionsHandlerType.DOWNLOAD_FILE, { event, fileId, fileName })
        }
        handlePreviewFile={(fileId: string) => onUpdate(QuestionsHandlerType.PREVIEW_FILE, fileId)}
      />
      <Col span={24}>
        <Card>
          <Row type={"flex"} justify={"start"} gutter={[40, 40]} align={"top"}>
            <Col span={7}>
              <AntTitle level={4}>Assessment Comment</AntTitle>
            </Col>
            <Col span={12}>
              <GenericMentions
                placeholder="Add a comment...."
                value={comments}
                id={"comments"}
                uri="users"
                method="list"
                searchField="email"
                onChange={(comment: any) => {
                  setComments(comment);
                }}
                onBlur={(e: any) => {
                  let newQuiz = quiz;
                  newQuiz!.comments = comments;
                  onUpdate(QuestionsHandlerType.UPDATE_QUIZ, { newQuiz, should: true });
                }}
                optionRenderer={(user: any, email: any) => (
                  <>
                    <NameAvatar name={email} />
                    {email}
                  </>
                )}
                optionValueTransformer={(option: { email: any }) => `[${option.email}]`}
              />
            </Col>
            <Col span={4}>
              <Button
                onClick={e => {
                  let newQuiz = quiz;
                  newQuiz!.comments = comments;
                  onUpdate(QuestionsHandlerType.UPDATE_QUIZ, { newQuiz, should: true });
                }}
                type={"primary"}>
                Save Comment
              </Button>
            </Col>
          </Row>
        </Card>
      </Col>
    </Row>
  );
};

export default AnswerQuestionnaireComponent;
