import { Icon, notification, Upload } from "antd";
import { AttachmentItem } from "../../../../shared-resources/components/attachment-item/attachment-item.component";
import PreviewerComponent from "../../../../shared-resources/components/previewer/previewer";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { AntButton, AntCard, AntForm, AntFormItem, AntInput, AntSelect } from "../../../../shared-resources/components";
import { RestKB } from "../../../../classes/RestKB";
import { SelectRestapi } from "../../../../shared-resources/helpers";
import queryString from "query-string";
import { RouteComponentProps } from "react-router-dom";
import { checkTemplateNameExists } from "../../../helpers/checkTemplateNameExits";
import { EMPTY_FIELD_WARNING, NAME_EXISTS_ERROR, URL_WARNING } from "../../../../constants/formWarnings";
import { useDispatch, useSelector } from "react-redux";
import ErrorWrapper from "../../../../hoc/errorWrapper";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { validateURL } from "../../../../utils/stringUtils";
import { getBaseUrl, TEMPLATE_ROUTES } from "../../../../constants/routePaths";
import Loader from "../../../../components/Loader/Loader";
import {
  bpsCreate,
  bpsFileUpload,
  bpsGet,
  bpsUpdate,
  filesDelete,
  filesGet,
  genericList,
  restapiClear,
  tagsBulkList,
  tagsCreate
} from "reduxConfigs/actions/restapi";
import "./knowledgebase-create-edit.style.scss";
import { KbType } from "../knowledgebase-list/knowledgebase-list.page";
import { formClear, formUpdateField } from "reduxConfigs/actions/formActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { getTagsSelector, KbsSelectorState } from "reduxConfigs/selectors/restapiSelector";
import { getKBForm } from "reduxConfigs/selectors/formSelector";
import { RestTags } from "../../../../classes/RestTags";
import { debounce } from "lodash";
import { isFileTypeAllowed, showTypeNotAllowedMessage } from "helper/files.helper";

export interface KBCreateEditPageProps extends RouteComponentProps {
  page: any;
  tagsList: [];
  className: string;
  kb_form: any;
  rest_api: any;
}

enum FieldName {
  NAME = "name",
  VALUE = "value"
}

enum OperationMode {
  CREATE = "create",
  UPDATE = "update"
}

enum UploadStatus {
  NOT_STARTED = "not_started",
  UPLOADING = "uploading",
  UPLOADED = "uploaded"
}

const KBCreateEditPage: React.FC<KBCreateEditPageProps> = (props: KBCreateEditPageProps) => {
  const values = queryString.parse(props.location?.search);

  const nameField = useRef<string>();
  const valuesField = useRef<string>();
  const typeField = useRef<string>();
  const metaField = useRef<string>();
  const nameExists = useRef<boolean>(false);
  const buttonClicked = useRef<boolean>(false);
  const createdTags = useRef<any[]>([]);
  const fileUploadStatus = useRef<UploadStatus>(UploadStatus.NOT_STARTED);
  const editingKbId = useRef<any>(queryString.parse(props.location?.search)?.kb);

  const dispatch = useDispatch();
  const [loading, setLoading] = useState<boolean>(false);
  const [tagsLoading, setTagsLoading] = useState<boolean>(false);
  const [tagsSelect, setSelectedTags] = useState<any[]>([]);
  const [createLoading, setCreateLoading] = useState<boolean>(false);
  const [created, setCreated] = useState<boolean>(false);
  const [updateBtnStatus, setUpdateBtnStatus] = useState<boolean>(!!editingKbId.current);
  const [checking_name, setCheckingName] = useState<boolean>(false);
  const [kbFile, setKbFile] = useState<any>();
  const [showPreviewer, setShowPreviewer] = useState<boolean>(false);
  const [deleteFileId, setDeleteFileId] = useState<any>();
  const [errorTexts, setErrorTexts] = useState<any>({});
  const [isFieldTouched, setIsFieldTouched] = useState<any>({});
  const [addedFile, setAddedFile] = useState<any>(null);
  const [readyToSave, setReadyToSave] = useState<any>(false);
  const [listOfUploadFiles, setListOfUploadedFiles] = useState<any[]>([]);

  const apiState = useSelector(state => KbsSelectorState(state, editingKbId.current));
  const kbFormState = useSelector(state => getKBForm(state));
  const tagsState = useSelector(state => getTagsSelector(state));
  const pageState = useSelector(state => pageSettings(state));

  const debounceCheck = useCallback(
    // preserve the same instance on all re-renders
    debounce(() => {
      checkTemplateName();
    }, 500),
    []
  );

  useEffect(() => {
    if (values.kb) {
      editingKbId.current = values.kb;
      dispatch(bpsGet(editingKbId.current));
      setLoading(true);
    }
    dispatch(
      setPageSettings(props.location.pathname, {
        title: "Templates",
        action_buttons: {
          create_update: {
            type: "primary",
            label: editingKbId.current ? "Update KB" : "Create KB",
            hasClicked: false
          }
        }
      })
    );
    return () => {
      // @ts-ignore
      dispatch(restapiClear("bestpractices", "get", "-1"));

      // @ts-ignore
      dispatch(restapiClear("bestpractices", OperationMode.CREATE, "-1"));

      // @ts-ignore
      dispatch(restapiClear("bestpractices", OperationMode.UPDATE, "-1"));

      // @ts-ignore
      dispatch(restapiClear("bestpractices", "upload", "-1"));
      dispatch(formClear("kb_form"));
      dispatch(clearPageSettings(props.location.pathname));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    dispatch(setPageButtonAction(props.location.pathname, "create_update", { disabled: !updateBtnStatus }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [updateBtnStatus]);

  useEffect(() => {
    if (checking_name) {
      const { loading, error } = apiState?.list;
      if (!loading && !error) {
        const data = apiState?.list?.data?.records;
        const prevName = kbFormState.name;
        setCheckingName(false);
        nameExists.current = checkTemplateNameExists(prevName, data);
        validateForm();
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [checking_name, apiState?.list]);

  useEffect(() => {
    typeField.current = kbFormState.type;
    nameField.current = kbFormState.name;
    valuesField.current = kbFormState.value;
    metaField.current = kbFormState.metadata;
  }, [kbFormState]);

  useEffect(() => {
    if (readyToSave) {
      let bp = new RestKB(kbFormState);
      if (!editingKbId.current) {
        dispatch(bpsCreate(bp));
        notification.success({
          message: "Knowledge Base",
          description: "Creating Knowledge Base..."
        });
      } else {
        dispatch(bpsUpdate(editingKbId.current, bp));
        notification.success({
          message: "Knowledge Base",
          description: "Updating Knowledge Base..."
        });
      }
      dispatch(setPageButtonAction(props.location.pathname, "create_update", { hasClicked: false }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [readyToSave]);

  useEffect(() => {
    const page = pageState[props.location.pathname];
    if (page && page.hasOwnProperty("action_buttons")) {
      if (!!page.action_buttons?.create_update?.hasClicked) {
        if (buttonClicked.current) {
          return;
        }
        buttonClicked.current = true;
        createdTags.current = kbFormState.tags?.filter((tag: any) => tag.toString().includes("create:")) || [];
        deleteFileId
          ? dispatch(filesDelete(`kb/${editingKbId.current}/${deleteFileId}`))
          : console.log("No file to delete");
        if (createdTags.current.length) {
          uploadTags();
        } else {
          setReadyToSave(true);
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageState[props.location.pathname]]);

  const uploadFiles = () => {
    // triggers the file upload
    const editId = editingKbId.current || apiState?.create?.data?.id;
    if (kbFormState.type === KbType.FILE && kbFile) {
      notification.success({
        message: "File Upload",
        description: "Uploading file..."
      });
      dispatch(bpsFileUpload(editId, kbFile));
      fileUploadStatus.current = UploadStatus.UPLOADING;
    }
  };

  const uploadTags = () => {
    // triggers the newly created tag uploading
    createdTags.current.forEach((tag: any) => {
      let newTag = new RestTags();
      newTag.name = tag.replace("create:", "");
      dispatch(tagsCreate(newTag, tag));
    });
  };

  useEffect(() => {
    // reacts when file upload complete
    const editId = editingKbId.current || apiState?.create?.data?.id;
    if (apiState?.upload?.[editId]?.data) {
      let kbForm = kbFormState;
      if (kbForm?.value?.length) {
        // delete the old file from BE.
        dispatch(filesDelete(`kb/${editingKbId.current}/${kbForm.value}`));
      }
      dispatch(formUpdateField("kb_form", "value", apiState?.upload?.[editId]?.data?.id || "")); // update the form with file data.
      kbForm.value = apiState?.upload?.[editId]?.data?.id || "";
      notification.success({
        message: "File Upload",
        description: "File uploaded successfully"
      });
      dispatch(
        bpsUpdate(
          editId,
          new RestKB({
            ...kbFormState,
            value: apiState?.upload?.[editId]?.data?.id || ""
          })
        )
      );
      fileUploadStatus.current = UploadStatus.UPLOADED;
      setKbFile(undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.upload]);

  useEffect(() => {
    // triggers when tags are created.
    if (!buttonClicked.current) {
      return;
    }
    let isComplete = true;
    createdTags.current.forEach((tag: string) => {
      if (tagsState.create?.tag?.loading) {
        isComplete = false;
      }
    });
    if (isComplete) {
      const allTags = kbFormState.tags;
      let tagIds: any[] = allTags.filter((k: string) => k && !k?.startsWith("create"));
      createdTags.current.forEach(k => {
        tagIds = [...tagIds, tagsState.create?.[k]?.data?.id];
      });
      dispatch(formUpdateField("kb_form", "tags", tagIds));
      setReadyToSave(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tagsState.create]);

  useEffect(() => {
    // triggered when creation of KB is success
    if (!apiState?.create || !readyToSave) {
      return;
    }
    const { loading, error } = apiState?.create;
    if (loading === false && error === false) {
      if (kbFormState.type === KbType.FILE && kbFile && fileUploadStatus.current !== UploadStatus.UPLOADED) {
        uploadFiles();
      } else {
        notification.success({
          message: "Knowledge Base",
          description: "Knowledge Base created Successfully"
        });
        // @ts-ignore
        dispatch(restapiClear("bestpractices", "create", "-1"));
        dispatch(formClear("kb_form"));
        setCreated(true);
        setCreateLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.create]);

  useEffect(() => {
    // triggered when update of KB is success
    if (!apiState?.update || !readyToSave) {
      return;
    }
    const editId = editingKbId.current || apiState?.create?.data?.id;

    if (apiState?.update?.[editId]) {
      const { loading, error } = apiState?.update?.[editId];
      if (loading === false && error === false) {
        if (kbFormState.type === KbType.FILE && kbFile && fileUploadStatus.current !== UploadStatus.UPLOADED) {
          uploadFiles();
        } else {
          notification.success({
            message: "Knowledge Base",
            description: "Knowledge Base updated Successfully"
          });
          // @ts-ignore
          dispatch(restapiClear("bestpractices", "update", "-1"));
          dispatch(formClear("kb_form"));
          setCreated(true);
          setCreateLoading(false);
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiState?.update]);

  useEffect(() => {
    if (loading) {
      if (apiState?.get?.loading === false && apiState?.get?.error === false) {
        const kb = new RestKB(apiState?.get?.data);
        dispatch(formUpdateField("kb_form", "name", kb?.name));
        dispatch(formUpdateField("kb_form", "type", kb?.type));
        dispatch(formUpdateField("kb_form", "tags", kb?.tags));
        dispatch(formUpdateField("kb_form", "value", kb?.value));
        dispatch(formUpdateField("kb_form", "metadata", kb?.metadata));
        if (kb?.tags?.length > 0) {
          const filters = { filter: { tag_ids: kb?.tags } };
          dispatch(tagsBulkList(filters));
        }
        setTagsLoading(kb.tags.length > 0);
        setLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, apiState?.get]);

  useEffect(() => {
    // fetching tags to p
    if (tagsLoading) {
      if (tagsState.bulk?.[0]?.loading === false && tagsState.bulk?.[0]?.error === false) {
        const tagsSelected = tagsState.bulk?.["0"]?.data?.records?.map((tag: { name: any; id: any }) => ({
          label: tag?.name,
          key: tag?.id
        }));

        // @ts-ignore
        dispatch(restapiClear("tags", "bulk", "0"));
        setTagsLoading(false);
        setSelectedTags(tagsSelected);
      }
      return;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tagsLoading, tagsState.bulk?.[0]?.loading]);

  const onFieldChangeHandler = (field: string) => {
    return (e: any) => {
      switch (field) {
        case FieldName.NAME: {
          nameField.current = e.currentTarget ? e.currentTarget.value : e;
          setIsFieldTouched({ ...isFieldTouched, [FieldName.NAME]: true });
          break;
        }
        case FieldName.VALUE: {
          valuesField.current = e.currentTarget ? e.currentTarget.value : e;
          setIsFieldTouched({ ...isFieldTouched, [FieldName.VALUE]: true });
          break;
        }
      }
      dispatch(formUpdateField("kb_form", field, e.currentTarget ? e.currentTarget.value : e));
      if (field === FieldName.NAME) {
        debounceCheck();
      } else {
        validateForm();
      }
    };
  };

  const onTypeChange = (e: any) => {
    const kbForm = kbFormState;
    if (editingKbId.current && kbForm.type === KbType.FILE) {
      const deleteFileId = kbForm.value;
      setDeleteFileId(deleteFileId);
      dispatch(formUpdateField("kb_form", "value", ""));
      valuesField.current = undefined;
    }
    dispatch(formUpdateField("kb_form", "metadata", ""));
    metaField.current = undefined;
    dispatch(formUpdateField("kb_form", "type", e));
    typeField.current = e;
    validateForm();
  };

  const validateForm = () => {
    const finalError: any = {};
    if (typeField.current !== KbType.FILE && !valuesField.current?.length) {
      // not type file and value not present
      finalError["value"] = EMPTY_FIELD_WARNING;
    }
    if (!nameField.current?.length) {
      // name empty
      finalError["name"] = EMPTY_FIELD_WARNING;
    }
    if (nameExists.current) {
      // name already exists
      finalError["name"] = NAME_EXISTS_ERROR;
    }
    if (!finalError["value"] && typeField.current === KbType.LINK && !validateURL(valuesField.current)) {
      // invalid url
      finalError["value"] = URL_WARNING;
    }
    if (typeField.current === KbType.FILE && !metaField.current?.length) {
      // no file present
      finalError["file"] = EMPTY_FIELD_WARNING;
    }
    if (Object.keys(finalError)?.length) {
      setUpdateBtnStatus(false);
    } else {
      setUpdateBtnStatus(true);
    }
    setErrorTexts(finalError);
  };
  const checkTemplateName = () => {
    const filters = {
      filter: {
        partial: {
          name: nameField.current
        }
      }
    };
    dispatch(genericList("bestpractices", "list", filters));
    setCheckingName(true);
  };

  const handleFile = (file: any) => {
    if (file) {
      if (isFileTypeAllowed(file.name)) {
        dispatch(formUpdateField("kb_form", "metadata", file?.name || ""));
        setAddedFile(file || null);
        metaField.current = file?.name || null;
        validateForm();
        setKbFile(file);
        setListOfUploadedFiles([file]);

        return;
      } else {
        showTypeNotAllowedMessage();
      }
    }

    setAddedFile(null);
    setKbFile(undefined);
    setListOfUploadedFiles([]);
  };

  const downloadFile = () => {
    if (editingKbId.current) {
      dispatch(filesGet(`kb/${editingKbId.current}/${kbFormState.value}`));
    }
  };

  const getFields = (): any[] => {
    let fields = [
      {
        prefix: "kb",
        name: "name",
        label: "Name",
        isRequired: true,
        value: kbFormState.name
      },
      {
        prefix: "kb",
        name: "value",
        type: kbFormState.type === KbType.LINK ? "text" : "textarea",
        label: kbFormState.type === KbType.LINK ? "Link" : "Text",
        isRequired: true,
        value: kbFormState.value
      }
    ];

    if (kbFormState.type === KbType.FILE) {
      fields = fields.filter(field => field.name !== "value");
    }
    return fields;
  };

  if (created) {
    props.history.push(getBaseUrl() + TEMPLATE_ROUTES.KB.LIST);
  }

  if (loading || createLoading) {
    return <Loader />;
  }

  const class_name = props.className || "kb-edit-page";

  return (
    <div className={`flex direction-column align-center`}>
      <div className={`${class_name}__content`}>
        <AntCard title="Knowledge Base">
          <AntForm layout="vertical">
            <AntFormItem label="type" wrapperCol={{ span: 4 }}>
              <AntSelect
                className={`${class_name} ${class_name}__search-filter-type`}
                id="select-type"
                options={RestKB.TYPE.map(option => ({ label: option, value: option }))}
                value={kbFormState.type}
                isMulti={false}
                closeMenuOnSelect={true}
                onChange={onTypeChange}
              />
            </AntFormItem>
            {getFields().map(field => (
              <AntFormItem
                key={field.name}
                label={<span>{field.label}</span>}
                hasFeedback
                required={field.isRequired}
                validateStatus={errorTexts[field.name] ? "error" : "success"}
                help={errorTexts[field.name]}>
                <AntInput
                  value={field.value || ""}
                  label={field.label}
                  name={field.name}
                  rows={field.rows}
                  type={field.type || "text"}
                  onChange={onFieldChangeHandler(field.name)}
                />
              </AntFormItem>
            ))}
            {kbFormState.type === KbType.FILE && (
              <>
                <AntFormItem label={"File"} hasFeedback required={true}>
                  <Upload
                    multiple={false}
                    beforeUpload={(file, fileList) => {
                      handleFile(file);
                      return false;
                    }}
                    fileList={listOfUploadFiles}
                    onRemove={file => {
                      handleFile(undefined);
                    }}>
                    <AntButton disabled={kbFile}>
                      <Icon type="file" />
                      Upload File
                    </AntButton>
                  </Upload>
                </AntFormItem>
                {editingKbId.current && kbFormState.metadata.length > 0 && !addedFile && (
                  <AttachmentItem fileName={kbFormState.metadata} previewFile={() => setShowPreviewer(true)} />
                )}
              </>
            )}
            <AntFormItem label="Tags">
              <SelectRestapi
                style={{ width: "100%" }}
                placeholder="tags"
                uri="tags"
                fetchData={props.tagsList}
                searchField="name"
                value={tagsSelect}
                allowClear={false}
                mode="multiple"
                createOption={true}
                onChange={(options: any[]) => {
                  const tagOptions = options || [];
                  setSelectedTags(tagOptions);
                  dispatch(
                    formUpdateField(
                      "kb_form",
                      "tags",
                      options.map((option: { key: any }) => option.key)
                    )
                  );
                }}
              />
            </AntFormItem>
          </AntForm>
        </AntCard>
        {showPreviewer && (
          <PreviewerComponent
            onClose={() => setShowPreviewer(false)}
            onDownload={downloadFile}
            list={[
              {
                fileId: kbFormState.value,
                fileName: kbFormState.metadata || "download",
                fetchFileId: `kb/${editingKbId.current}/${kbFormState.value}`
              }
            ]}
            currentIndex={0}
          />
        )}
      </div>
    </div>
  );
};

// @ts-ignore
export default ErrorWrapper(KBCreateEditPage);
