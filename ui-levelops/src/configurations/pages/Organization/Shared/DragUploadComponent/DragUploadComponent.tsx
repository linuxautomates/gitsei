import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Icon, Upload, Radio, Spin } from "antd";
import { AntButton, AntText, SvgIcon } from "shared-resources/components";
import cx from "classnames";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";

import "./DragUploadComponent.scss";
import { CsvUploadType } from "../../Constants";
import { RadioChangeEvent } from "antd/lib/radio";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ORG_USER_SCHEMA_ID, orgUsersGenericSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { OrgUserImport, OrgUserSchemaGet } from "reduxConfigs/actions/restapi/orgUserAction";
import { WebRoutes } from "routes/WebRoutes";

const { Dragger } = Upload;
interface DragUploadComponentProps {
  callback?: (value: boolean) => void;
}

const DragUploadComponent: React.FC<DragUploadComponentProps> = (props: DragUploadComponentProps) => {
  const [type, setType] = useState<CsvUploadType>(CsvUploadType.REPLACE);
  const [fileName, setFileName] = useState<string>("");
  const [csvLoading, setCsvLoading] = useState<boolean>(false);
  const [csvError, setCscError] = useState<any>("");
  const [csvWarning, setCscWarning] = useState<any>("");
  const [csvUploaded, setCsvUploaded] = useState<boolean>(false);

  const dispatch = useDispatch();
  const history = useHistory();

  const importFileState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_import",
    method: "create",
    id: "import_file"
  });

  useEffect(() => {
    if (csvLoading) {
      const loading = get(importFileState, "loading", true);
      const error = get(importFileState, "error", true);
      if (!loading && !error) {
        const version = get(importFileState, ["data", "version"], undefined);
        setCsvLoading(false);
        setCsvUploaded(true);
        props?.callback?.(true);
        if (version) {
          dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema"));
          history.push(WebRoutes.organization_users_page.root(version));
        }
      }
      if (error) {
        const err = get(importFileState, ["data", "error"], "");
        setCsvLoading(false);
        setCscError(err);
      }
    }
  }, [importFileState]);

  const _onTypeChange = useCallback((value: RadioChangeEvent) => {
    setType(value.target.value);
  }, []);

  const errorContent = useMemo(() => {
    return (
      <div className="drag-upload-container-description__error">
        <p className="ant-upload-drag-icon">
          <Icon type={"close-circle"} />
        </p>
        <AntText>Error</AntText>
        <AntText>{csvError}</AntText>
      </div>
    );
  }, [csvError]);

  const dragDropHandler = (data: any) => {
    const _fileName = data && data.file && data.file.name;
    setFileName(_fileName);
    const _extension = _fileName.split(".")?.pop();
    if (_extension !== "csv") {
      return;
    }
    const payload = {
      file: data.file,
      ...data.data
    };
    setCsvLoading(true);
    dispatch(OrgUserImport(payload, "org_users_import", "create", "import_file"));
  };

  const waringContent = useMemo(() => {
    return (
      <div className="drag-upload-container-description__error">
        <SvgIcon icon={"close"} className="drag-upload-container-description__warning-svgIcon" />
        <AntText>Warning</AntText>
        <AntText className="mb-20">{csvWarning}</AntText>
        <AntButton type="secondary">Continue with Missing Email IDs</AntButton>
      </div>
    );
  }, [csvWarning]);

  const radioButtonContent = useMemo(() => {
    return (
      <div className="drag-uploaded-container mt-20">
        <AntText className="mb-10">Would you like to replace or update existing users?</AntText>
        <Radio.Group className="drag-uploaded-container__radio-container mb-30" onChange={_onTypeChange} value={type}>
          <Radio value={CsvUploadType.REPLACE}>Replace - This will overwrite the existing user list</Radio>
          <Radio value={CsvUploadType.UPDATE}>Update - This will merge with existing data</Radio>
        </Radio.Group>
      </div>
    );
  }, [type]);

  const finalContent = useMemo(() => {
    return (
      <div className="final-state-container m-70">
        <SvgIcon className={"version-button-icon"} icon="importCheck" />
        <AntText>Success! We have successfully imported & updated the users.</AntText>
      </div>
    );
  }, []);

  return !csvUploaded ? (
    <div className={cx("drag-upload-container", { "dragger-error": csvError, "dragger-warning": csvWarning })}>
      {!csvError && !csvWarning && radioButtonContent}
      <Dragger
        accept=".csv"
        multiple={false}
        showUploadList={false}
        height={320}
        data={{ import_mode: type, type: "csv" }}
        customRequest={(data: any) => dragDropHandler(data)}
        className="drag-upload-container-dragger">
        <div className="drag-upload-container-description">
          {csvError && errorContent}
          {csvWarning && waringContent}
          {!csvError && !csvWarning && !csvLoading && (
            <div>
              <p className="ant-upload-drag-icon">
                <Icon type={"inbox"} />
              </p>
              <span>
                Drag & drop or <u>click here</u> to upload a .CSV File
              </span>
            </div>
          )}
          {csvLoading && (
            <div className="drag-upload-container-spiner-container">
              <Spin className="mb-20" />
              Uploading file - {fileName}
            </div>
          )}
        </div>
      </Dragger>
    </div>
  ) : (
    <div>{csvUploaded && finalContent}</div>
  );
};

export default DragUploadComponent;
