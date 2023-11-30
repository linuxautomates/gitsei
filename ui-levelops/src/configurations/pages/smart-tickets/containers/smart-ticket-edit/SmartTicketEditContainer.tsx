import { RestWorkItem } from "classes/RestWorkItem";
import React, { useEffect, useState } from "react";
import { concat, debounce, filter, findIndex, get, map, reduce } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { workItemSelector } from "reduxConfigs/selectors/restapiSelector";
import {
  activitylogsList,
  filesGet,
  restapiClear,
  workItemDelete,
  workItemUdpate,
  workItemUpload
} from "reduxConfigs/actions/restapi";
import { getWorkitemDetailPage } from "constants/routePaths";
import { tagsSelector } from "reduxConfigs/selectors/tags.selector";
import { updatedWorkItem, WORKITEM_ACTION } from "./helper";
import { usersSelector } from "reduxConfigs/selectors/usersSelector";
import { filesGetState } from "reduxConfigs/selectors/files.selector";
import { CreateIssue } from "..";
import { SendKB } from "workitems/containers";
import SmartTicketEditBody from "./SmartTicketEditBody";
import { notification, Row } from "antd";
import { tagsGetOrCreate, usersGetOrCreate, workItemPatch } from "reduxConfigs/actions/restapi";
import { validateEmail } from "../../../../../utils/stringUtils";
import { RestTags } from "../../../../../classes/RestTags";
import SmartTicketEditFields from "./SmartTicketEditFields";
import "../smart-ticket-edit.styles.scss";
import { isFileTypeAllowed, showTypeNotAllowedMessage } from "helper/files.helper";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { FAILURE_UPLOAD_FILE } from "constants/error.constants";
import { content } from "html2canvas/dist/types/css/property-descriptors/content";

const RESTRICTED_EXTENSIONS = ["exe", "bin", "vbs", "scr", "iso", "dll", "dat", "elf"];

interface SmartTicketEditContainerProps {
  workItem: RestWorkItem;
  onReroute: (url: string) => void;
  onRefresh: () => void;
}

const SmartTicketEditContainer: React.FC<SmartTicketEditContainerProps> = (props: SmartTicketEditContainerProps) => {
  const dispatch = useDispatch();
  const [showPreviewer, setShowPreviewer] = useState<boolean>(false);
  const [currentPreviewIndex, setCurrentPreviewIndex] = useState<number>(-1);
  const [workItem, setWorkItem] = useState<RestWorkItem>(props.workItem);
  const [dirty, setDirty] = useState<boolean>(false);
  const [createModal, setCreateModal] = useState<boolean>(false);
  const [creatingTags, setCreatingTags] = useState<boolean>(false);
  const [loadingTags, setLoadingTags] = useState<boolean>(props.workItem.tag_ids?.length > 0);
  const [creatingUsers, setCreatingUsers] = useState<boolean>(false);
  const [tags, setTags] = useState<Array<any>>([]);
  const [showSendBp, setShowSendBp] = useState<boolean>(false);
  const [downloadingFiles, setDownloadingFiles] = useState<boolean>(false);
  const [downloadingFilesIds, setDownloadingFilesIds] = useState<Array<string>>([]);
  const [ticketType, setTicketType] = useState<string>(props.workItem.ticket_type);
  const [uploadingFiles, setUploadingFiles] = useState<boolean>(false);
  const [attachmentsToUpload, setAttachmentsToUpload] = useState<any>({});
  const [updatingProduct, setUpdatingProduct] = useState<boolean>(false);
  const [deletingWorkItem, setWorkItemDeleting] = useState<boolean>(false);
  const [values, setValues] = useState<any>(false);
  const [templateFields, setTemplateFields] = useState<any[]>([]);
  const [showMore, setShowMore] = useState<boolean>(false);
  const [firstShow, setFirstShow] = useState<boolean>(false);
  const [loading_complete, setLoadingComplete] = useState<boolean>(false);
  const [skipFirstRender, setSkipFirstRender] = useState<boolean>(true);

  const [attachments, setAttachments] = useState<Array<any>>(
    map(props.workItem.attachments || [], (file, index) => ({
      ...file,
      uid: file.upload_id,
      name: file.file_name || `Attachment ${index}`,
      status: "done"
    }))
  );

  const [assignees, setAssignees] = useState<Array<any>>(
    map(props.workItem.assignees || [], (assignee: any) => ({ key: assignee.user_id, label: assignee.user_email }))
  );
  const [product, setProduct] = useState<{ key: string; label: string }>({ key: props.workItem.product_id, label: "" });

  const tagsRestState = useSelector(tagsSelector);
  const workItemRestState = useSelector(workItemSelector);
  const usersRestState = useSelector(usersSelector);
  const filesRestState = useSelector(filesGetState);

  const debouncedRefresh = debounce(props.onRefresh, 500);
  const debounceAuditLogs = debounce((data: any) => dispatch(activitylogsList(data)), 500);

  const getUpdatedWorkItem = () => {
    return updatedWorkItem({ attachments, assignees, tags, ticketType, values, templateFields, workItem });
  };

  useEffect(() => {
    if (loadingTags) {
      const loading = get(tagsRestState, ["list", props.workItem.id, "loading"], true);
      const error = get(tagsRestState, ["list", props.workItem.id, "error"], false);
      if (!loading && !error) {
        const records = get(tagsRestState, ["list", props.workItem.id, "data", "records"], []);
        setTags(map(records, tag => ({ key: tag.id, label: tag.name })));
        setLoadingTags(false);
      }
    }
  }, [tagsRestState, loadingTags]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!loading_complete) {
      let values: any = {};
      const templateFields = workItem.ticket_data_values.map((field: any) => {
        values[field.ticket_field_id] = {};
        if (field.values && field.values.length > 0) {
          values[field.ticket_field_id] = {
            value: field.values.map((v: any) => {
              return v.value;
            })
          };
        }
        return field;
      });

      setTemplateFields(templateFields);
      setValues(values);
      setLoadingComplete(true);
    }
  }, [props]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (creatingTags) {
      const loading = get(tagsRestState, ["getOrCreate", 0, "loading"], true);
      const error = get(tagsRestState, ["getOrCreate", 0, "error"], false);
      if (!loading && !error) {
        const newtags = get(tagsRestState, ["getOrCreate", 0, "data"], []);
        dispatch(restapiClear("tags", "getOrCreate", "-1"));
        const mappedTags = concat(
          filter(tags, (tag: any) => !tag.key?.includes("create:")),
          map(newtags, (tag: any) => ({ key: tag.id, label: tag.name.replace("create:", "") }))
        );
        setTags(mappedTags);
        setCreatingTags(false);
      }
    }
  }, [tagsRestState, creatingTags]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (creatingUsers) {
      const loading = get(usersRestState, ["getOrCreate", 0, "loading"], true);
      const error = get(usersRestState, ["getOrCreate", 0, "error"], false);
      if (!loading && !error) {
        const newUsers = get(usersRestState, ["getOrCreate", 0, "data"], []);
        dispatch(restapiClear("users", "getOrCreate", "-1"));
        const payload = getUpdatedWorkItem();
        const newAssignees = concat(
          filter(payload.assignees, (assignee: any) => !assignee.user_id?.includes("create:")),
          map(newUsers, (user: any) => ({ user_id: user.id, user_email: user.email }))
        );
        setAssignees(map(newAssignees, (assignee: any) => ({ key: assignee.user_id, label: assignee.user_email })));
        setCreatingUsers(false);
      }
    }
  }, [usersRestState, creatingUsers]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (uploadingFiles) {
      const uploadIds = Object.keys(attachmentsToUpload);
      const remainingUploads: any = {};
      let shouldUpdate = false;

      if (!uploadIds.length) {
        setUploadingFiles(false);
      } else {
        for (const _uploadId of uploadIds) {
          const file = attachmentsToUpload[_uploadId];
          const loading = get(workItemRestState, ["workItemRestApi", "upload", _uploadId, "loading"], true);
          const error = get(workItemRestState, ["workItemRestApi", "upload", _uploadId, "error"], false);

          if (!loading && !error) {
            const fileId = get(workItemRestState, ["workItemRestApi", "upload", _uploadId, "data", "id"], undefined);
            setAttachments((prev: any[]) => [
              ...prev,
              {
                upload_id: fileId,
                file_name: file.attachment_name
              }
            ]);

            dispatch(restapiClear("workitem", "upload", _uploadId));
            shouldUpdate = true;
          } else {
            remainingUploads[_uploadId] = file;
          }
        }

        if (shouldUpdate) {
          dispatch(activitylogsList({ filter: { target_items: [props.workItem.id] } }));
        }

        setAttachmentsToUpload(remainingUploads);
        setDirty(true);
      }
    }

    if (downloadingFiles) {
      const fileIds = map(downloadingFilesIds, (f: any) => f.file_id);
      if (fileIds.length) {
        const newIds = reduce(
          fileIds,
          (acc: any, fileId: any) => {
            const loading = get(filesRestState, [fileId, "loading"], true);
            const error = get(filesRestState, [fileId, "error"], false);
            if (!loading && !error) {
              acc.push(filter(downloadingFilesIds, (f: any) => f.file_id !== fileId));
            }
            return acc;
          },
          []
        );
        setDownloadingFilesIds(newIds);
      } else {
        setDownloadingFiles(false);
      }
    }
  }, [uploadingFiles, downloadingFiles, filesRestState, workItemRestState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (updatingProduct) {
      const loading = get(workItemRestState, ["workItemRestApi", "patch", workItem.id, "loading"], true);
      const error = get(workItemRestState, ["workItemRestApi", "patch", workItem.id, "error"], false);
      if (!loading && !error) {
        const vanity_id = get(
          workItemRestState,
          ["workItemRestApi", "patch", workItem.id, "data", "vanity_id"],
          undefined
        );
        let url = `${getWorkitemDetailPage()}?workitem=${vanity_id}`;
        props.onReroute(url);
        setUpdatingProduct(false);
        setDirty(true);
      }
    }
  }, [updatingProduct, workItemRestState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (deletingWorkItem) {
      const loading = get(workItemRestState, ["workItemRestApi", "delete", workItem.id, "loading"], true);
      const error = get(workItemRestState, ["workItemRestApi", "delete", workItem.id, "error"], false);
      if (!loading && !error) {
        props.onReroute(`${getWorkitemDetailPage()}?delete=${workItem.vanity_id}`);
        setWorkItemDeleting(false);
      }
    }
  }, [deletingWorkItem, workItemRestState]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (dirty) {
      dispatch(workItemUdpate(workItem.id, getUpdatedWorkItem()));
      debounceAuditLogs({ filter: { target_items: [workItem.id] } });
    }
  }, [dirty]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    // This hook is use to do api call after useState variable are updated
    if (creatingUsers || creatingTags || loadingTags) {
      return;
    }

    // skipping first call to prevent API call of save
    if (!skipFirstRender) {
      handleUpdateWorkItem();
    } else {
      setSkipFirstRender(false);
    }
  }, [assignees, tags, attachments, ticketType]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleUpdateWorkItem = () => {
    dispatch(workItemUdpate(workItem.id, getUpdatedWorkItem()));
    debounceAuditLogs({ filter: { target_items: [workItem.id] } });
  };

  const onUpdate = (action: WORKITEM_ACTION, payload: any) => {
    switch (action) {
      case WORKITEM_ACTION.WORKITEM_DELETE:
        setWorkItemDeleting(true);
        dispatch(workItemDelete(workItem.id));
        break;

      case WORKITEM_ACTION.WORKITEM_MENU_CLICK:
        switch (payload.key) {
          case "send_kb":
            setShowSendBp(true);
            break;
          case "sub_ticket":
            setCreateModal(true);
            break;
          default:
            break;
        }
        break;

      case WORKITEM_ACTION.WORKITEM_NAME_UPDATE:
        setWorkItem((prev: any) => {
          prev.title = payload;
          return prev;
        });
        handleUpdateWorkItem();
        debouncedRefresh();
        break;

      case WORKITEM_ACTION.WORKITEM_DESCRIPTION_UPDATE:
        setWorkItem((prev: any) => {
          prev.description = payload;
          return prev;
        });
        handleUpdateWorkItem();
        break;

      case WORKITEM_ACTION.WORKITEM_FILE_CHANGE:
        const remainingFiles = payload.filter((a: any) => a.status !== "removed");
        if (payload.length - remainingFiles.length > 0) {
          setAttachments(remainingFiles);
          return;
        }
        setAttachments(payload);
        break;

      case WORKITEM_ACTION.WORKITEM_FILE_ADD:
        try {
          if (payload.name && isFileTypeAllowed(payload.name)) {
            const _uploadId = workItem.id + "-" + payload.uid;
            setUploadingFiles(true);
            setAttachmentsToUpload((prev: any) => {
              return {
                ...prev,
                [_uploadId]: {
                  attachment_name: payload.name
                }
              };
            });
            dispatch(workItemUpload(_uploadId, workItem.id, payload));
          } else {
            showTypeNotAllowedMessage();
          }
        } catch (e) {
          handleError({
            showNotfication: true,
            message: FAILURE_UPLOAD_FILE,
            bugsnag: {
              severity: severityTypes.WARNING,
              context: issueContextTypes.FILES,
              data: { e, action }
            }
          });
        }
        break;
      case WORKITEM_ACTION.WORKITEM_FILE_DOWNLOAD:
        if (findIndex(downloadingFilesIds, (f: any) => f.file_id === payload.fileId) !== -1) {
          return;
        }
        dispatch(filesGet(payload.fileId, payload.fileName));
        setDownloadingFiles(true);
        setDownloadingFilesIds((prev: any) => {
          return [...prev, { file_id: payload.fileId, file_name: payload.fileName }];
        });
        break;

      case WORKITEM_ACTION.WORKITEM_FILE_REMOVE:
        const remainingattachmentsFiles = attachments.filter(a => a.upload_id !== payload.upload_id);
        const WorkItem = workItem;
        WorkItem.attachments = remainingattachmentsFiles;
        setAttachments(remainingattachmentsFiles);
        setWorkItem(WorkItem);
        break;

      case WORKITEM_ACTION.WORKITEM_FILE_PREVIEW:
        const index = findIndex(attachments, f => f.upload_id === payload.upload_id);
        if (index !== -1) {
          setShowPreviewer(true);
          setCurrentPreviewIndex(index);
        }
        break;

      case WORKITEM_ACTION.WORKITEM_FILE_CLOSE_PREVIEW:
        setShowPreviewer(false);
        setCurrentPreviewIndex(-1);
        break;

      case WORKITEM_ACTION.TICKET_TYPE_CHANGE:
        setTicketType(payload);
        debouncedRefresh();
        break;

      case WORKITEM_ACTION.ASSIGNEE_CHANGE:
        const newUsers = map(
          filter(payload, (option: any) => option?.key?.includes("create:")),
          (user: any) => user?.key
        );
        if (newUsers.length > 0) {
          const inValidEmails = reduce(
            newUsers,
            (acc: any, option: any) => {
              if (!validateEmail((option || "").replace("create:", ""))) {
                acc.push(option);
              }
              return acc;
            },
            []
          );
          if (inValidEmails.length) {
            notification.error({ message: "Invalid Email" });
          } else {
            setCreatingUsers(true);
            setAssignees(payload);
            dispatch(usersGetOrCreate(newUsers));
          }
        } else {
          setAssignees([...payload]);
          debouncedRefresh();
        }
        break;

      case WORKITEM_ACTION.WORKITME_STATUS_CHANGE:
        const workItemState = workItem;
        workItemState.status = payload.label;
        workItemState.state_id = payload.key;
        setWorkItem(workItemState);
        handleUpdateWorkItem();
        debouncedRefresh();
        break;

      case WORKITEM_ACTION.WORKITEM_DUE_DATE_CHANGE:
        let workItemDuedate = workItem;
        workItemDuedate.due_at = payload !== null ? Math.ceil(payload.valueOf() / 1000) : payload;
        setWorkItem(workItemDuedate);
        handleUpdateWorkItem();
        break;

      case WORKITEM_ACTION.UPDATED_TAGS:
        if (payload) {
          const { newTags } = RestTags.getNewAndExistingTags(payload);
          if (newTags.length > 0) {
            const tagsToCreate = map(newTags, t => t.key);
            setCreatingTags(true);
            setTags(payload);
            dispatch(tagsGetOrCreate(tagsToCreate));
          } else {
            setTags(payload);
          }
        }
        break;

      case WORKITEM_ACTION.WORKITEM_PRODUCT_CHANGE:
        if (!payload) {
          return null;
        }
        let workItemProduct = workItem;
        workItemProduct.product_id = payload.key;
        setWorkItem(workItemProduct);
        setProduct(payload);
        setUpdatingProduct(true);
        dispatch(
          workItemPatch(workItemProduct.id, {
            id: workItemProduct.id,
            product_id: payload.key
          })
        );
        debouncedRefresh();
        break;

      case WORKITEM_ACTION.TOGGLE_MORE_CUSTOM_FIELDS:
        setShowMore(!payload);
        setFirstShow(true);
        break;

      case WORKITEM_ACTION.UPDATE_CUSTOM_FIELDS:
        setValues((prev: any) => {
          return {
            ...prev,
            ...payload
          };
        });
        handleUpdateWorkItem();
        break;
    }
  };

  return (
    <>
      <Row gutter={[40, 0]} type={"flex"} className="smart-ticket-edit">
        <SmartTicketEditBody
          workItem={workItem}
          attachments={attachments}
          showPreviewer={showPreviewer}
          currentPreviewIndex={currentPreviewIndex}
          onReroute={props.onReroute}
          onUpdate={onUpdate}
          onRefresh={props.onRefresh}
        />
        <SmartTicketEditFields
          workItem={workItem}
          templateFields={templateFields}
          tags={tags}
          product={product}
          values={values}
          showMore={showMore}
          firstShow={firstShow}
          ticketType={ticketType}
          assignees={assignees}
          onUpdate={onUpdate}
        />
      </Row>
      {createModal && (
        <CreateIssue
          parent_id={props.workItem.id}
          onCancelEvent={() => setCreateModal((prev: boolean) => !prev)}
          onSuccessEvent={(newWorkId: any) => {
            setCreateModal((prev: boolean) => !prev);
            props.onRefresh();
            props.onReroute(`${getWorkitemDetailPage()}?workitem=${newWorkId}`);
          }}
          isVisible
        />
      )}
      {showSendBp && (
        <SendKB visible workItemid={workItem.id} onCancel={() => setShowSendBp(false)} artifact={workItem.artifact} />
      )}
    </>
  );
};

export default SmartTicketEditContainer;
