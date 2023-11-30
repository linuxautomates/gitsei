import React, { useMemo, useState, useEffect } from "react";
import { Icon, Upload, Radio } from "antd";
import cx from "classnames";
import ".././Shared/DragUploadComponent/DragUploadComponent.scss";
import { AntText, SvgIcon } from "shared-resources/components";
import { readString } from "react-papaparse";
import { getUserFromCSV } from "../Helpers/OrgUnit.helper";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";

const { Dragger } = Upload;

interface OrgUnitCSVUploadWrapperProps {
  handleCSVUpload: (fileName: string, ids: string[]) => void;
  handleSetFileData: (data: string[]) => void;
  csvUploaded: boolean;
}

const OrgUnitCSVUploadWrapper: React.FC<OrgUnitCSVUploadWrapperProps> = ({
  csvUploaded,
  handleCSVUpload,
  handleSetFileData
}) => {
  const [error, setError] = useState<string>("");

  const users = useParamSelector(getOrgUnitUtility, { utility: "users" });

  const errorContent = useMemo(() => {
    return (
      <div className="drag-upload-container-description__org_unit_error">
        <p className="ant-upload-drag-icon">
          <Icon type={"close-circle"} />
        </p>
        <AntText className="error">Error</AntText>
        <AntText>{error}</AntText>
      </div>
    );
  }, [error]);

  const finalContent = useMemo(() => {
    return (
      <div className="final-state-container m-70">
        <SvgIcon className={"version-button-icon"} icon="importCheck" />
        <AntText>Success! We have successfully imported & updated the users.</AntText>
      </div>
    );
  }, []);

  const dragDropHandler = (data: any) => {
    const _fileName = data && data.file && data.file.name;
    const splittedFileName: string[] = _fileName.split(".");
    const _extension = splittedFileName.length ? splittedFileName[splittedFileName.length - 1] : "";

    if (_extension !== "csv") {
      setError("Uploaded a non .csv file!");
      return;
    }

    const fileReader = new FileReader();
    fileReader.onload = (e: any) => {
      if (e && e.target) {
        let _dataArray: any[] = readString(e.target.result as string).data;
        const value = getUserFromCSV(_dataArray || [], users || []);
        if (typeof value === "boolean" && !value) {
          setError("Contributor Emails does not exists!");
          return;
        }
        handleSetFileData(_dataArray);
        handleCSVUpload(_fileName, value);
      }
    };

    fileReader.readAsText(data.file);
  };

  return !csvUploaded ? (
    <div className={cx("drag-upload-container", { "dragger-error": error })}>
      {error ? (
        errorContent
      ) : (
        <Dragger
          accept=".csv"
          multiple={false}
          showUploadList={false}
          height={320}
          data={{ type: "csv" }}
          customRequest={dragDropHandler}
          className="drag-upload-container-dragger">
          <div className="drag-upload-container-description">
            <div>
              <p className="ant-upload-drag-icon">
                <Icon type={"inbox"} />
              </p>
              <span>
                Drag & drop or <u>click here</u> to upload a .CSV File
              </span>
            </div>
          </div>
        </Dragger>
      )}
    </div>
  ) : (
    finalContent
  );
};

export default OrgUnitCSVUploadWrapper;
